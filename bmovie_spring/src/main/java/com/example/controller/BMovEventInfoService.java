package com.example.controller;

import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

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

    public RankingsList handleUserRatingRequest(String genre, int k) {
        String genreKey = genre + "Rating";

        try (Jedis jedis = pool.getResource()) { // try with resources block!
            if (!jedis.exists(genreKey)) {
                RankingsList emptyRankings = new RankingsList("N.A.");
                return emptyRankings;

            } else {
                int min = (int) Math.min(k, jedis.zcard(genreKey));
                List<String> imdbIDs = new ArrayList<>(jedis.zrevrange(genreKey, 0, min));
                String status = "Top-" + String.valueOf(min);
                RankingsList rankingsList = new RankingsList(status);
                rankingsList.setImdbIDs(imdbIDs);
                return rankingsList;
            }
        }
    }

    public RankingsList handleGrossEarningsRequest(int k) {
        String grossKey = "TOTAL_COLLECTION_EARNINGS";

        try (Jedis jedis = pool.getResource()) { // try with resources block!
            if (!jedis.exists(grossKey)) {
                RankingsList emptyRankings = new RankingsList("N.A.");
                return emptyRankings;

            } else {
                int min = (int) Math.min(k, jedis.zcard(grossKey));
                List<String> imdbIDs = new ArrayList<>(jedis.zrevrange(grossKey, 0, min));
                String status = "Top-" + String.valueOf(min);
                RankingsList rankingsList = new RankingsList(status);
                rankingsList.setImdbIDs(imdbIDs);
                return rankingsList;
            }
        }
    }

    public RankingsList handleViewerShipRequest(int k) {
        String viewsKey = "TOTAL_VIEWS";

        try (Jedis jedis = pool.getResource()) {
            if (!jedis.exists(viewsKey)) {
                RankingsList emptyRankings = new RankingsList("N.A.");
                return emptyRankings;

            } else {
                int min = (int) Math.min(k, jedis.zcard(viewsKey));
                List<String> imdbIDs = new ArrayList<>(jedis.zrevrange(viewsKey, 0, min));
                String status = "Top-" + String.valueOf(min);
                RankingsList rankingsList = new RankingsList(status);
                rankingsList.setImdbIDs(imdbIDs);
                return rankingsList;
            }
        }
    }
}
