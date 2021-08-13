package com.example.entrypoint;

import com.example.controller.BMovie;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import okhttp3.OkHttpClient;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class BMovieDataInserter {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private Map<String, BMovie> bMovieData;
    private OkHttpClient client = new OkHttpClient();
    private static final String MONGO_HOST = "localhost";
    private static final int MONGO_PORT = 27017;
    private static final String DB_NAME = "ad";

    public BMovieDataInserter() {
        BMovieDataAggregator bma = new BMovieDataAggregator();
        bma.aggregateMovieInfo();
        bMovieData = bma.getbMovieData();
    }

    public void connect() {
        String connectionString = String.format("mongodb://%s:%d", MONGO_HOST, MONGO_PORT);
        mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase(DB_NAME);
    }

    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public void storeBMovieDatainDB() throws JsonProcessingException {
        MongoCollection<Document> bMovieDataCol = database.getCollection("bMovieDataCol");
        ObjectMapper mapper = new ObjectMapper();

        if (bMovieDataCol.find().into(new ArrayList<>()).size() == 0) {
            for (BMovie bm : bMovieData.values()) {
                String jsonStr = mapper.writeValueAsString(bm);
                Document doc = Document.parse(jsonStr);
                bMovieDataCol.insertOne(doc);
            }
        }
    }

    public void addIndexesInBMovieCollection() {
        if (database.listCollections().into(new ArrayList<>()).size() == 0) {
            database.createCollection("bMovieDataCol");
        }
        MongoCollection<Document> bMovieDataCol = database.getCollection("bMovieDataCol");

        if (bMovieDataCol.listIndexes().into(new ArrayList<>()).size() == 1) {
            bMovieDataCol.createIndex(Indexes.ascending("imdbRating"));

            bMovieDataCol.createIndex(Indexes.ascending("duration"));

            bMovieDataCol.createIndex(Indexes.ascending(Arrays.asList("imdbRating", "duration")));

            bMovieDataCol.createIndex(Indexes.ascending("yearOfRelease"));

            bMovieDataCol.createIndex(Indexes.ascending(Arrays.asList("yearOfRelease", "imdbRating")));

            bMovieDataCol.createIndex(Indexes.ascending(Arrays.asList("yearOfRelease", "duration")));

            bMovieDataCol.createIndex(Indexes.ascending(Arrays.asList("yearOfRelease", "imdbRating", "duration")));
        }
    }
}
