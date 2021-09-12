package com.example.kafka;

import com.example.utils.BMovieConfigProps;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EventAggregatorDriver {
    public static void startConsumerThreads() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        JedisPool jPool = new JedisPool(buildPoolConfig(), BMovieConfigProps.getRedisAddress(), 6379);
        RedisDataInserter inserter = new RedisDataInserter();

        List<EventAggregator> processors = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            // initialize our consumer thread
            EventAggregator aggregator = new EventAggregator(jPool, inserter);
            aggregator.initializeAggregator();
            processors.add(aggregator);
            executor.submit(aggregator);
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
            jPool.close();
        }));
    }

    private static JedisPoolConfig buildPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(20);
        poolConfig.setMinIdle(8);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(120).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setMaxWaitMillis(2000);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }
}
