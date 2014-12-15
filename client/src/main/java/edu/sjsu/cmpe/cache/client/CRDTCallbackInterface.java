package edu.sjsu.cmpe.cache.client;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;

public interface CRDTCallbackInterface {

    void completed (HttpResponse<JsonNode> response);

    void failed (Exception e);
}