package com.example.generator;

public class EventsGeneratorDriver {
    public static void generateEvents() throws InterruptedException {
        BMovieEventGenerator generator = new BMovieEventGenerator();
        generator.startWorkerThread();
        //generator.generateBMovieSeenEvents();
        generator.generateFiniteSetBMovieSeenEvents();
        generator.terminateMongoConnection();
    }
}
