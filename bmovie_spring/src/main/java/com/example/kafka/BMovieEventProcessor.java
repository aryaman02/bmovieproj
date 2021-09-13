package com.example.kafka;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Serdes;
import org.bson.Document;

public class BMovieEventProcessor implements ConsumerRebalanceListener, Runnable {
    private static final class EventPublished implements Callback {
        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            // TODO Auto-generated method stub
            System.out.println("Received ack; offset: " + metadata.offset() + " partitionId: " + metadata.partition());
        }
    }

    private static final int POLL_WAIT_TIME = 7000;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private KafkaConsumer<String, BMovieSeenEvent> eventProcessor;
    private String clientId;

    private static final String CONSUME_TOPIC_NAME = "bmovie_seen_events";
    private static final String CONNECTION_STRING = "%s:9092";
    private Set<String> indianStates;
    private final MongoConnectionAdapter mongoAdapter = new MongoConnectionAdapter();
    private MongoDatabase database;

    private KafkaProducer<String, BMovieGenreEvent> eventPublisher;
    private final EventPublished callback = new EventPublished();
    private static final String PUBLISH_TOPIC_NAME = "bmovie_genre_events";
    private static final int NUM_PARTITIONS = 12;

    private final ObjectMapper m = new ObjectMapper();

    public BMovieEventProcessor(Set<String> uniqueStrs) {
        indianStates = uniqueStrs;
        String mongoHost = BMovieConfigProps.getMongoDBAddress();
        String mongoDB = System.getProperty("mongodb.database", "ad");
        mongoAdapter.connect(mongoHost, mongoDB);
        database = mongoAdapter.getDatabase();
    }

    public void initializeConsumer() {
        clientId =  "EventProcessor-" + UUID.randomUUID().toString();
        Properties props = createPropsConsumer();
        eventProcessor = new KafkaConsumer<>(props);
        eventProcessor.subscribe(Arrays.asList(CONSUME_TOPIC_NAME), this);
    }

    public void initializeProducer() {
        Properties props = createPropsProducer();
        eventPublisher = new KafkaProducer<>(props);
    }

    public void stopConsumer() {
        running.set(false);
        eventProcessor.wakeup();
    }

    private Properties createPropsConsumer() {
        Properties props = new Properties();

        props.put("bootstrap.servers", String.format(CONNECTION_STRING, BMovieConfigProps.getKafkaAddress()));
        props.put("key.deserializer", Serdes.String().deserializer().getClass().getName());
        props.put("value.deserializer", "com.example.kafka.SeenEventDeserializer");

        props.put("group.id", "event_processor");
        props.put("application.id", "event_processor");
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

    private Properties createPropsProducer() {
        Properties props = new Properties();

        props.put("bootstrap.servers", String.format(CONNECTION_STRING, BMovieConfigProps.getKafkaAddress()));
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "com.example.kafka.KafkaJsonSerializer");
        props.put("acks", "1");
        props.put("batch.size", 256);
        props.put("linger.ms", 5);
        return props;
    }

    private void publishGenreEvent(BMovieGenreEvent event) {
        int partition = -1;

        try {
            partition = (Math.abs(event.getbMovGenre().hashCode())) % NUM_PARTITIONS; // re keying technique!!
            ProducerRecord<String, BMovieGenreEvent> record = new ProducerRecord<>(PUBLISH_TOPIC_NAME, partition, null, event);
            eventPublisher.send(record, callback);
        } catch (RuntimeException ex) {
            String errMessage = String.format("Exception while publishing message %s on partition %d", event, partition);
            throw new RuntimeException(errMessage, ex);
        }
    }

    @Override
    public void run() {

        System.out.println("Starting Kafka consumer: " + clientId);
        try {
            MongoCollection<Document> bMovieDataCol = database.getCollection("bMovieDataCol");

            // LOOP
            while (running.get()) {
                // 1. wait for message
                ConsumerRecords<String, BMovieSeenEvent> records = eventProcessor.poll(Duration.ofMillis(POLL_WAIT_TIME));
                // 2. Get message
                if (records == null) {
                    continue;
                }

                // 3. consume and process (record count will be 1, because max.poll.records = 1
                for (ConsumerRecord<String, BMovieSeenEvent> record : records) {
                    System.out.println("Consumer with ClientId: " + clientId + " read message: " + record.value() + "  from partition: " + record.partition());
                    BMovieSeenEvent userEvent = record.value();

                    if (userEvent.getUserRating() >= 0.0 && userEvent.getUserRating() <= 10.0 && indianStates.contains(userEvent.getbMovSeenLocation()) &&
                        userEvent.getTicketPrice() >= 30 && userEvent.getTicketPrice() <= 180) { // if bMovSeenEvent is good

                        List<String> genres = (List<String>) bMovieDataCol.find(new Document("imdbID", userEvent.getImdbID())).into(new ArrayList<>()).get(0).get("genres");

                        for (String genre : genres) {
                            BMovieGenreEvent genreEvent = new BMovieGenreEvent(userEvent.getImdbID(), userEvent.getUserRating(), userEvent.getbMovSeenLocation(),
                                userEvent.getbMovSeenDate(), userEvent.getTicketPrice(), userEvent.getViewerAge(), genre);

                            publishGenreEvent(genreEvent); // publishing genre event to bmovie_genre_events topic
                        }
                    }
                    // 4. Commit (ack) -> increment the offset in Kafka
                    eventProcessor.commitAsync();
                }
            }
        } catch (WakeupException ex) {
        } catch (Exception ex) {
            System.out.println("Exception in Kafka message processing loop " + ex.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        eventProcessor.close();
        eventPublisher.close();
        mongoAdapter.disconnect();
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        partitions.forEach(partition -> System.out.println("For consumer: " + clientId + " Partition revoked: " + partition.partition()));
        eventProcessor.commitSync();
        MongoCollection<Document> offsetCol = database.getCollection("partition_offsets_col");

        for (TopicPartition tp : partitions) {
            if (tp.topic().equals(CONSUME_TOPIC_NAME)) {
                long currOffset = eventProcessor.position(tp);
                BasicDBObject newDocument = new BasicDBObject();
                newDocument.append("$set", new BasicDBObject().append("offset", currOffset));

                BasicDBObject searchQuery = new BasicDBObject().append("partitionID", tp.partition());

                offsetCol.updateOne(searchQuery, newDocument);
            }
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        if (database.listCollections().into(new ArrayList<>()).size() == 0) {
            database.createCollection("partition_offsets_col");
        }

        MongoCollection<Document> offsetCol = database.getCollection("partition_offsets_col");

        for (TopicPartition tp : partitions) {
            List<Document> queryDocs = offsetCol.find(new Document("partitionID", tp.partition())).into(new ArrayList<>());

            if (tp.topic().equals(CONSUME_TOPIC_NAME)) {
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

                    eventProcessor.seek(tp, 0L); // optional line - always read from beginning of partition at start

                } else {
                    Document doc = queryDocs.get(0);
                    long startingOffset = doc.getLong("offset");
                    eventProcessor.seek(tp, startingOffset);
                }
            }
        }

        partitions.forEach(partition -> System.out.println("For consumer: " + clientId + " Partition assigned: " + partition.partition()));
    }
}
