package com.example.utils;

public class BMovieConfigProps {
    public static String getKafkaAddress() {
        String address = System.getenv("KAFKA_HOST");
        System.out.println("kafka address: " + address);
        if (address == null) return "0.0.0.0";
        else return address;
    }

    public static String getMongoDBAddress() {
        String address = System.getenv("MONGO_HOST");
        System.out.println("mongo address: " + address);
        if (address == null) return "0.0.0.0";
        else return address;
    }

    public static String getRedisAddress() {
        String address = System.getenv("REDIS_HOST");
        System.out.println("redis address: " + address);
        if (address == null) return "0.0.0.0";
        else return address;
    }

    public static String getMinIOHost() {
        String address = System.getenv("MINIO_HOST");
        System.out.println("minio address: " + address);
        if (address == null) return "0.0.0.0";
        else return address;
    }
}
