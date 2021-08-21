package com.example.kafka;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConsumerDriver {
    public static void startConsumerThreads() throws InterruptedException {

        ExecutorService executor = Executors.newCachedThreadPool();

        //int partition_count = 12;

        for (int i = 0; i < 5; i++) {
            KConsumer consumer = new KConsumer();
            consumer.initialize();
            executor.submit(consumer);
        }
        Thread.sleep(3600 * 1000L);

        executor.shutdown();
    }
}
