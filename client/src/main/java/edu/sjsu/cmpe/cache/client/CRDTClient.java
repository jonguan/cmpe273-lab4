package edu.sjsu.cmpe.cache.client;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
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
//    public AtomicInteger successCount;


    public CRDTClient() {

        servers = new ArrayList(3);
        servers.add("http://localhost:3000");
        servers.add("http://localhost:3001");
        servers.add("http://localhost:3002");
    }

// Syncrhonous implementation
    public boolean put(long key, String value) {
        successServers = new ArrayList(3);
//        successCount = new AtomicInteger();

        for (final String serverUrl : servers) {
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
            } else {
                System.out.println("Added " + value + " to " + serverUrl);
                successServers.add(serverUrl);
//                successCount.incrementAndGet();
            }

        }

        boolean isSuccess = Math.round((float)successServers.size() / servers.size()) == 1;

        if (! isSuccess) {
            // Send delete for the same key
            delete(key, value);
        }
        return isSuccess;
    }


    public String get(long key) {
        HttpResponse<JsonNode> response = null;
        try {
            response = Unirest.get(servers.get(0) + "/cache/{key}")
                    .header("accept", "application/json")
                    .routeParam("key", Long.toString(key)).asJson();
        } catch (UnirestException e) {
            System.err.println(e);
        }

        String value = null;
        if (response != null && response.getStatus() == 200) {
            value = response.getBody().getObject().getString("value");
        }

        return value;
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