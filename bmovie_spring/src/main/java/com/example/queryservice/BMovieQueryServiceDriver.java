package com.example.queryservice;

import com.example.controller.MongoConnectionAdapter;
import com.example.utils.BMovieConfigProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoDatabase;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bson.Document;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BMovieQueryServiceDriver {
    public static final int GRPC_PORT = 8746;

    private static final Logger logger = LoggerFactory.getLogger(BMovieQueryServiceDriver.class);
    private Server server = null;

    private final MongoConnectionAdapter mongoConnectionAdapter = new MongoConnectionAdapter();
    private MongoDatabase database;
    private OkHttpClient client = new OkHttpClient();
    private final ObjectMapper m = new ObjectMapper();
    private Set<String> bMovieGenres = new HashSet<>();

    public void start() {
        String mongoHost = BMovieConfigProps.getMongoDBAddress();
        String mongoDB = System.getProperty("mongodb.database", "ad");
        mongoConnectionAdapter.connect(mongoHost, mongoDB);
        database = mongoConnectionAdapter.getDatabase();

        server = ServerBuilder.forPort(GRPC_PORT).addService(new BMovieDescriptionHandler(this)).addService(new BMovieImgHandler(this)).
            addService(new BMovieQueryHandler(this)).addService(new BMovieGenresHandler(this)).build();

        try {
            server.start();
            logger.info("gRPC server started at port: {}", GRPC_PORT);
        } catch (IOException e) {
            logger.error("Failed to start gRPC server to start at port: " + GRPC_PORT, e);
        }
    }

    public void waitUntilShutdown() {
        if (server != null) {
            try {
                server.awaitTermination();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        mongoConnectionAdapter.disconnect();

        if (server != null) {
            server.shutdown();
            logger.info("gRPC server shut down");
        }
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public ObjectMapper getJsonMapper() {
        return m;
    }

    public Set<String> getBMovieGenres() {
        return bMovieGenres;
    }

    public void populateGenres() {
        // assuming we have a populated db
        List<Document> query = database.getCollection("bMovieDataCol").find().into(new ArrayList<>());

        for (Document doc : query) {
            List<String> genres = (List<String>) doc.get("genres");

            for (String g : genres) {
                bMovieGenres.add(g);
            }
        }
    }
    public boolean isValidGenre(String genre) {
        return (bMovieGenres.contains(genre));
    }

    public void updateBMovieGenres(String genre) {
        bMovieGenres.add(genre);
    }
}


