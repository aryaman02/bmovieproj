package com.example.controller;

import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;

import java.util.ArrayList;
import java.util.List;

@Service
public class BMovEventInfoService {
    private JedisPool pool;

    public void initializePool(String host, int port) {
        pool = new JedisPool(host, port);
    }

    public void closePool() {
        pool.close();
    }

    public List<RankingEntry> handleUserRatingRequest(String genre, int k) {
        String genreKey = genre + "Rating";
        List<RankingEntry> rankings = new ArrayList<>();

        try (Jedis jedis = pool.getResource()) { // try with resources block!
            if (jedis.exists(genreKey)) {
                int min = (int) Math.min(k, jedis.zcard(genreKey));
                List<Tuple> entries = new ArrayList<>(jedis.zrevrangeWithScores(genreKey, 0, min-1));

                for (Tuple t : entries) {
                    rankings.add(new RankingEntry(t.getElement(), t.getScore()));
                }
            }
        }
        return rankings;
    }

    public List<RankingEntry> handleGrossEarningsRequest(int k) {
        String grossKey = "TOTAL_COLLECTION_EARNINGS";
        List<RankingEntry> rankings = new ArrayList<>();

        try (Jedis jedis = pool.getResource()) { // try with resources block!
            if (jedis.exists(grossKey)) {
                int min = (int) Math.min(k, jedis.zcard(grossKey));
                List<Tuple> entries = new ArrayList<>(jedis.zrevrangeWithScores(grossKey, 0, min-1));

                for (Tuple t : entries) {
                    rankings.add(new RankingEntry(t.getElement(), t.getScore()));
                }
            }
        }
        return rankings;
    }

    public List<RankingEntry> handleViewerShipRequest(int k) {
        String viewsKey = "TOTAL_VIEWS";

        List<RankingEntry> rankings = new ArrayList<>();

        try (Jedis jedis = pool.getResource()) { // try with resources block!
            if (jedis.exists(viewsKey)) {
                int min = (int) Math.min(k, jedis.zcard(viewsKey));
                List<Tuple> entries = new ArrayList<>(jedis.zrevrangeWithScores(viewsKey, 0, min-1));

                for (Tuple t : entries) {
                    rankings.add(new RankingEntry(t.getElement(), t.getScore()));
                }
            }
        }
        return rankings;
    }
}
