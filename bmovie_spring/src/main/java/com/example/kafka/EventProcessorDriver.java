package com.example.kafka;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EventProcessorDriver {
    public static void startConsumerThreads() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        Set<String> indianStates = new HashSet<>();
        populateIndianStates(indianStates);
        List<BMovieEventProcessor> processors = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            BMovieEventProcessor processor = new BMovieEventProcessor(indianStates);
            processor.initializeProducer();
            processor.initializeConsumer();
            processors.add(processor);
            executor.submit(processor);
        }
        //Thread.sleep(3600 * 10000L);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("shutting down");

            processors.forEach(processor -> processor.stopConsumer());
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

    }

    private static void populateIndianStates(Set<String> listOfStrings) {
        List<String> indianStates = Arrays
            .asList("AN", "AP", "AR", "AS", "BR", "CH", "CT", "DN", "DD", "DL", "GA", "GJ", "HR",
                "HP", "JK", "JH", "KA", "KL", "LD", "MP", "MH", "MN", "ML", "MZ", "NL", "OR", "PY", "PB", "RJ", "SK", "TN", "TG", "TR", "UP", "UT", "WB");

        listOfStrings.addAll(indianStates);
    }
}
