package com.example.kafka;

import redis.clients.jedis.Jedis;

public class RedisDataInserter {
    public synchronized void storeNetGrossOfBMovie(BMovieGenreEvent event, Jedis jedis) {
        String imdbID = event.getImdbID();
        String timeStamp = event.getbMovSeenDate();
        String grossKey = imdbID + "MONEY";
        String collectionEarningsKey = "TOTAL_COLLECTION_EARNINGS";

        if (!jedis.exists(grossKey)) {
            int ticketPrice = event.getTicketPrice();
            jedis.hset(grossKey, timeStamp, String.valueOf(ticketPrice));
            jedis.zadd(collectionEarningsKey, ticketPrice, imdbID);

        } else if (jedis.exists(grossKey) && !jedis.hexists(grossKey, timeStamp)) {
            int ticketPrice = event.getTicketPrice();
            jedis.hset(grossKey, timeStamp, String.valueOf(ticketPrice));
            jedis.zincrby(collectionEarningsKey, ticketPrice, imdbID);
        }
    }

    public synchronized void storeTotalViewsOfBMovie(BMovieGenreEvent event, Jedis jedis) {
        String imdbID = event.getImdbID();
        String timeStamp = event.getbMovSeenDate();
        String viewsKey = imdbID + "VIEWS";
        String totalViewsKey = "TOTAL_VIEWS";

        if (!jedis.exists(viewsKey)) {
            jedis.hset(viewsKey, timeStamp, "WATCHED");
            jedis.zadd(totalViewsKey, 1, imdbID);

        } else if (jedis.exists(viewsKey) && !jedis.hexists(viewsKey, timeStamp)) {
            jedis.hset(viewsKey, timeStamp, "WATCHED");
            jedis.zincrby(totalViewsKey, 1, imdbID);
        }
    }
}
