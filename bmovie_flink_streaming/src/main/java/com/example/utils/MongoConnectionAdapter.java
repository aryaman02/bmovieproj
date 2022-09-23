package com.example.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class MongoConnectionAdapter implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(MongoConnectionAdapter.class);

    private MongoClient mongoClient;
    private MongoDatabase database;
    private final ObjectMapper m = new ObjectMapper();

    public MongoDatabase getDatabase() {
        return database;
    }

    public void connect(String mongoHost, String databaseName) {
        String connectionString = String.format("mongodb://%s:%d",  mongoHost, 27017);
        mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase(databaseName);
    }

    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public ObjectMapper getJsonMapper() {
        return m;
    }
}
