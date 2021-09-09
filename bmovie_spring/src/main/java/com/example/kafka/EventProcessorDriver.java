package com.example.kafka;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventProcessorDriver {
    public static void startConsumerThreads() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        Set<String> indianStates = new HashSet<>();
        populateIndianStates(indianStates);

        for (int i = 0; i < 3; i++) {
            BMovieEventProcessor processor = new BMovieEventProcessor(indianStates);
            processor.initializeProducer();
            processor.initializeConsumer();
            executor.submit(processor);
        }
        Thread.sleep(3600 * 10000L);

        executor.shutdown();
    }

    private static void populateIndianStates(Set<String> listOfStrings) {
        List<String> indianStates = Arrays
            .asList("AN", "AP", "AR", "AS", "BR", "CH", "CT", "DN", "DD", "DL", "GA", "GJ", "HR",
                "HP", "JK", "JH", "KA", "KL", "LD", "MP", "MH", "MN", "ML", "MZ", "NL", "OR", "PY", "PB", "RJ", "SK", "TN", "TG", "TR", "UP", "UT", "WB");

        listOfStrings.addAll(indianStates);
    }
}
