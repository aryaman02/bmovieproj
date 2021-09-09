package com.example.kafka;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventAggregatorDriver {
    public static void startConsumerThreads() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        JedisPool jPool = new JedisPool(buildPoolConfig(), "0.0.0.0", 6379);

        for (int i = 0; i < 3; i++) {
            // initialize our consumer thread
            EventAggregator aggregator = new EventAggregator(jPool);
            aggregator.initializeAggregator();
            executor.submit(aggregator);
        }
        Thread.sleep(3600 * 10000L);

        executor.shutdown();
        jPool.close();
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
