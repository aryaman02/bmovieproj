package com.example.kafka;

import com.example.controller.MongoConnectionAdapter;
import com.example.generator.BMovieSeenEvent;
import com.example.utils.BMovieConfigProps;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Serdes;
import org.bson.Document;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventAggregator implements ConsumerRebalanceListener, Runnable {
    private static final int POLL_WAIT_TIME = 10000;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private KafkaConsumer<String, BMovieGenreEvent> aggregator;
    private String clientId;

    private static final String TOPIC_NAME = "bmovie_genre_events";
    private static final String CONNECTION_STRING = "%s:9092";
    private final MongoConnectionAdapter mongoAdapter = new MongoConnectionAdapter();
    private MongoDatabase database;

    private JedisPool pool;
    private RedisDataInserter dataInserter;
    private final ObjectMapper m = new ObjectMapper();

    public EventAggregator(JedisPool pool, RedisDataInserter inserter) {
        this.pool = pool;
        dataInserter = inserter;
        String mongoHost = BMovieConfigProps.getMongoDBAddress();
        String mongoDB = System.getProperty("mongodb.database", "ad");
        mongoAdapter.connect(mongoHost, mongoDB);
        database = mongoAdapter.getDatabase();
    }

    public void initializeAggregator() {
        clientId =  "Aggregator-" + UUID.randomUUID().toString();
        Properties props = createPropsConsumer();
        aggregator = new KafkaConsumer<>(props);
        aggregator.subscribe(Arrays.asList(TOPIC_NAME), this);
    }

    public void stopConsumer() {
        running.set(false);
        aggregator.wakeup();
    }

    private Properties createPropsConsumer() {
        Properties props = new Properties();

        //String connectionString = String.format(CONNECTION_STRING, BMovieConfigProps.getKafkaAddress());
        //System.out.println("Kafka address: " + connectionString);
        props.put("bootstrap.servers", "0.0.0.0:9093");
        props.put("key.deserializer", Serdes.String().deserializer().getClass().getName());
        props.put("value.deserializer", "com.example.kafka.GenreEventDeserializer");

        props.put("group.id", "aggregator");
        props.put("application.id", "aggregator");
        /**
         * We want to commit the offset to Kafka only after processing is complete.
         */
        props.put("enable.auto.commit", "false");
        /**
         * We want to process one message at a time.
         */
        props.put("max.poll.records", 1);
        props.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.CooperativeStickyAssignor");
        props.put("client.id", clientId);
        props.put("scheduled.rebalance.max.delay.ms", 5000);
        return props;
    }

    @Override
    public void run() {
        System.out.println("Starting Kafka consumer: " + clientId);

        try {
            // LOOP
            while (running.get()) {
                // 1. wait for message
                ConsumerRecords<String, BMovieGenreEvent> records = aggregator.poll(Duration.ofMillis(POLL_WAIT_TIME));
                // 2. Get message
                if (records == null) {
                    continue;
                }

                // 3. consume and process (record count will be 1, because max.poll.records = 1
                for (ConsumerRecord<String, BMovieGenreEvent> record : records) {
                    System.out.println("Consumer with ClientId: " + clientId + " read message: " + record.value() + "  from partition: " + record.partition());
                    BMovieGenreEvent genreEvent = record.value();

                    // use this genre event to store all the right kind of data you need in redis
                    try (Jedis jedis = pool.getResource()) { // try with resources block!
                        storeAvgUserRatingOfBMovie(genreEvent, jedis);
                        dataInserter.storeNetGrossOfBMovie(genreEvent, jedis);
                        dataInserter.storeTotalViewsOfBMovie(genreEvent, jedis);
                    }

                    // 4. Commit (ack) -> increment the offset in Kafka
                    aggregator.commitAsync();
                }
            }
        } catch (WakeupException ex) {
        } catch (Exception ex) {
            System.out.println("Exception in Kafka message processing loop " + ex.getMessage());
        } finally {
            cleanup();
        }
    }

    private void storeAvgUserRatingOfBMovie(BMovieGenreEvent event, Jedis jedis) {
        String genre = event.getbMovGenre();
        String imdbID = event.getImdbID();
        String genreKey = genre + "Rating";

        if (!jedis.exists(genre) || (jedis.exists(genre) && !jedis.hexists(genre, imdbID))) {
            double userRating = event.getUserRating();
            String str = new StringBuilder().append(String.valueOf(userRating)).append(" ").append(String.valueOf(1)).toString();
            jedis.hset(genre, imdbID, str);

            jedis.zadd(genreKey, userRating, imdbID); // add avg user rating for a movie for a particular genre in sorted set

        } else {
            double accumRating = Double.parseDouble(jedis.hget(genre, imdbID).split(" ")[0]);
            int numWatched = Integer.parseInt(jedis.hget(genre, imdbID).split(" ")[1]);
            accumRating += event.getUserRating();
            numWatched++;
            String str = new StringBuilder().append(String.valueOf(accumRating)).append(" ").append(String.valueOf(numWatched)).toString();
            jedis.hset(genre, imdbID, str);

            jedis.zadd(genreKey, accumRating/numWatched, imdbID); // add avg user rating for a movie for a particular genre in sorted set
        }
    }

    private void cleanup() {
        aggregator.close();
        mongoAdapter.disconnect();
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        partitions.forEach(partition -> System.out.println("For consumer: " + clientId + " Partition revoked: " + partition.partition()));
        aggregator.commitSync();
        MongoCollection<Document> offsetCol = database.getCollection("partition_offsets_col2");

        for (TopicPartition tp : partitions) {
            if (tp.topic().equals(TOPIC_NAME)) {
                long currOffset = aggregator.position(tp);
                BasicDBObject newDocument = new BasicDBObject();

                System.out.println("for partition " + tp.partition() + "  storing offset: " + currOffset);
                newDocument.append("$set", new BasicDBObject().append("offset", currOffset));

                BasicDBObject searchQuery = new BasicDBObject().append("partitionID", tp.partition());

                offsetCol.updateOne(searchQuery, newDocument);
            }
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        if (database.listCollections().into(new ArrayList<>()).size() == 0) {
            database.createCollection("partition_offsets_col2");
        }

        MongoCollection<Document> offsetCol = database.getCollection("partition_offsets_col2");

        for (TopicPartition tp : partitions) {
            List<Document> queryDocs = offsetCol.find(new Document("partitionID", tp.partition())).into(new ArrayList<>());

            if (tp.topic().equals(TOPIC_NAME)) {
                if (queryDocs.size() == 0) {
                    PartitionOffset pOffset = new PartitionOffset(tp.partition(), 0);
                    String jsonStr = null;
                    try {
                        jsonStr = m.writeValueAsString(pOffset);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    Document doc = Document.parse(jsonStr);
                    offsetCol.insertOne(doc);

                    aggregator.seek(tp, 0L); // optional line - always read from beginning of partition at start

                } else {
                    Document doc = queryDocs.get(0);
                    long startingOffset = doc.getLong("offset");
                    System.out.println("for partition " + tp.partition() + "  seeking to offset: " + startingOffset);
                    aggregator.seek(tp, startingOffset);
                }
            }
        }

        partitions.forEach(partition -> System.out.println("For consumer: " + clientId + " Partition assigned: " + partition.partition()));
    }
}
