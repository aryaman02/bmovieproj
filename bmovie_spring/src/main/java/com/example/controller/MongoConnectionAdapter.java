package com.example.controller;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoConnectionAdapter {

    private MongoClient mongoClient;
    private MongoDatabase database;

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
}
