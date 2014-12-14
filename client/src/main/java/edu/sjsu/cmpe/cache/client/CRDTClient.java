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
    public AtomicInteger successCount;

    public CRDTClient() {

        servers = new ArrayList(3);
        servers.add("http://localhost:3000");
        servers.add("http://localhost:3001");
        servers.add("http://localhost:3002");
    }


    public boolean put(long key, String value) {
//        final CountDownLatch countDownLatch = new CountDownLatch(servers.size());

        successCount = new AtomicInteger();
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

            if (response.getCode() != 200) {
                System.out.println("Failed to add to the cache.");
            } else {
                System.out.println("Added " + value + "to " + serverUrl);
                successCount.incrementAndGet();
            }

//            countDownLatch.countDown();
        }

//        countDownLatch.await();

        return (Math.round(successCount.floatValue() / servers.size()) == 1);
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
        String value = response.getBody().getObject().getString("value");

        return value;
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