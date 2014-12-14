package edu.sjsu.cmpe.cache.client;

import java.util.*;
import java.lang.*;
import java.io.*;

public class Client {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Cache Client...");
        CRDTClient crdtClient = new CRDTClient();

        // First HTTP PUT call to store “a” to key 1. (Then, sleep for ~30 seconds so that you will have enough time to stop the server A)
        boolean result = crdtClient.put(1, "poo");
        System.out.println("result is " + result);
//        sleep(30);
//        System.out.println("Step 1: put(1 => a); sleeping 30s");
//
//
//        // Second HTTP PUT call to update key 1 value to “b”. (Then, sleep again for another ~30 seconds while bringing the server A back)
//        crdtClient.put(1, "b");
//        sleep(30);
//        System.out.println("Step 2: put(1 => b); sleeping 30s");
//
//
//        // Final HTTP GET call to retrieve key “1” value.
        String value = crdtClient.get(1);
        System.out.println("Step 3: get(1) => " + value);

        System.out.println("Exiting Client...");
    }

}
