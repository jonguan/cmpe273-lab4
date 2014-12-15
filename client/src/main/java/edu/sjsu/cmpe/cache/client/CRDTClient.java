package edu.sjsu.cmpe.cache.client;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.*;
import java.io.*;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.options.Options;


public class CRDTClient  {

    private ArrayList<String> servers;
    private ArrayList<String> successServers;
    private ConcurrentHashMap<String, ArrayList<String>> dictResults;

    public CRDTClient() {

        servers = new ArrayList(3);
        servers.add("http://localhost:3000");
        servers.add("http://localhost:3001");
        servers.add("http://localhost:3002");
    }

// Syncrhonous implementation
    public boolean put(long key, String value) {
        successServers = new ArrayList(3);

        for (final String serverUrl : servers) {
            boolean result = put(key, value, serverUrl);
            if (result) {
                successServers.add(serverUrl);
            }
        }

        boolean isSuccess = Math.round((float)successServers.size() / servers.size()) == 1;

        if (! isSuccess) {
            // Send delete for the same key
            delete(key, value);
        }
        return isSuccess;
    }

    public boolean put (long key, String value, String serverUrl) {
        HttpResponse<JsonNode> response = null;
        try {
            response = Unirest
                    .put(serverUrl + "/cache/{key}/{value}")
                    .header("accept", "application/json")
                    .routeParam("key", Long.toString(key))
                    .routeParam("value", value).asJson();
        } catch (UnirestException e) {
            System.err.println(e);
        }

        if (response == null || response.getStatus() != 200) {
            System.out.println("Failed to add to the cache.");
            return false;
        } else {
            System.out.println("Added " + value + " to " + serverUrl);

            return true;
        }
    }

    // dictResult = {"value" : [serverUrl1, serverUrl2...]]}
    public String get(long key) {

        String rightValue = null;
        dictResults = new ConcurrentHashMap<String, ArrayList<String>>();

        for (final String serverUrl : servers) {
            HttpResponse<JsonNode> response = null;
            try {
                response = Unirest.get(serverUrl + "/cache/{key}")
                        .header("accept", "application/json")
                        .routeParam("key", Long.toString(key)).asJson();
            } catch (UnirestException e) {
                System.err.println(e);
            }

            String value = null;
            if (response != null && response.getStatus() == 200) {
                value = response.getBody().getObject().getString("value");
//                System.out.println("value from server " + serverUrl + "is " + value);
                ArrayList serversWithValue = dictResults.get(value);
                if (serversWithValue == null) {
                    serversWithValue = new ArrayList(3);
                }

                serversWithValue.add(serverUrl);

                // Save Arraylist of servers into dictResults
                dictResults.put(value, serversWithValue);

                // Initialize rightValue with something
                rightValue = value;
            }
        }

//        System.out.println("dictResults: " + dictResults);

        // Discrepancy in results (either more than one value gotten, or null gotten somewhere)
        if (dictResults.keySet().size() > 1 || dictResults.get(rightValue).size() != servers.size()) {
            // Most frequent value in dictResults
            ArrayList<String> maxValues = maxKeyForTable(dictResults);
//            System.out.println("maxValues: " + maxValues);
            if (maxValues.size() == 1) {
                // Max value - iterate through dict keys to repair
                rightValue = maxValues.get(0);

                ArrayList<String> repairServers = new ArrayList(servers);
                repairServers.removeAll(dictResults.get(rightValue));
//                System.out.println("repairServers: " + repairServers);

                for (String serverUrl : repairServers) {
                    // Repair all servers that don't have the correct value
                    System.out.println("repairing: " + serverUrl + " value: " + rightValue);
                    put(key, rightValue, serverUrl);
                }

            } else {
                // Multiple or no max keys? - do nothing
            }
        }


        return rightValue;
    }

    // Returns array of keys with the maximum value
    // If array contains only 1 value, then it is the highest value in the hash map
    public ArrayList<String> maxKeyForTable(ConcurrentHashMap<String, ArrayList<String>> table) {
        ArrayList<String> maxKeys= new ArrayList<String>();
        int maxValue = -1;
        for(Map.Entry<String, ArrayList<String>> entry : table.entrySet()) {
            if(entry.getValue().size() > maxValue) {
                maxKeys.clear(); /* New max remove all current keys */
                maxKeys.add(entry.getKey());
                maxValue = entry.getValue().size();
            }
            else if(entry.getValue().size() == maxValue)
            {
                maxKeys.add(entry.getKey());
            }
        }
        return maxKeys;
    }

    public void delete(long key, String value) {

        for (final String serverUrl : successServers) {
            HttpResponse<JsonNode> response = null;
            try {
                response = Unirest
                        .delete(serverUrl + "/cache/{key}")
                        .header("accept", "application/json")
                        .routeParam("key", Long.toString(key)).asJson();
            } catch (UnirestException e) {
                System.err.println(e);
            }

            System.out.println("response is " + response);

            if (response == null || response.getStatus() != 204) {
                System.out.println("Failed to delete from the cache.");
            } else {
                System.out.println("Deleted " + value + " from " + serverUrl);

            }

        }
    }

//    public boolean put(long key, String value) {
//        final CountDownLatch countDownLatch = new CountDownLatch(servers.size());
//        successCount = new AtomicInteger();
//
//        for (final String serverUrl : servers) {
//            Future<HttpResponse<JsonNode>> future = Unirest.post(serverUrl + "/cache/{key}/{value}")
//                    .header("accept", "application/json")
//                    .routeParam("key", Long.toString(key))
//                    .routeParam("value", value).asJson()
//                    .asJsonAsync(new Callback<JsonNode>() {
//
//                        public void failed(UnirestException e) {
//                            System.out.println("The request has failed: " + serverUrl);
//                            countDownLatch.countDown();
//                        }
//
//                        public void completed(HttpResponse<JsonNode> response) {
//                            int code = response.getStatus();
//                            if (code == 200) {
//                                successCount.incrementAndGet();
//                            }
//
//                            countDownLatch.countDown();
//                        }
//
//                        public void cancelled() {
//                            System.out.println("The request has been cancelled");
//                        }
//
//                    });
//        }
//
//        // Block the thread until all responses gotten
//        countDownLatch.await();
//
//        return (Math.round(successCount.floatValue() / servers.size()) == 1);
//    }




}