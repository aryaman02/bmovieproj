package com.example.kafka;

import com.example.generator.BMovieSeenEvent;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class BMovieSeenEventsPublisher {
    private static final class EventPublished implements Callback {
        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            // TODO Auto-generated method stub
            System.out.println("Received ack; offset: " + metadata.offset() + " partitionId: " + metadata.partition());
        }
    }

    private KafkaProducer<String, BMovieSeenEvent> producer;
    private final EventPublished callback = new EventPublished();
    private static final String CONNECTION_STRING = "0.0.0.0:9092";
    private static final String TOPIC_NAME = "bmovie_seen_events";
    private static final int NUM_PARTITIONS = 12;

    public void initialize() {
        Properties props = createProps();
        producer = new KafkaProducer<>(props);
    }

    public void cleanup() {
        producer.close();
    }

    public void publishBMovEvent(BMovieSeenEvent event) {
        int partition = -1;

        try {
            partition = (Math.abs(event.getImdbID().hashCode())) % NUM_PARTITIONS;
            ProducerRecord<String, BMovieSeenEvent> record = new ProducerRecord<>(TOPIC_NAME, partition, null, event);
            producer.send(record, callback);
        } catch (RuntimeException ex) {
            String errMessage = String.format("Exception while publishing message %s on partition %d", event, partition);
            throw new RuntimeException(errMessage, ex);
        }
    }


    private Properties createProps() {
        Properties props = new Properties();

        props.put("bootstrap.servers", CONNECTION_STRING);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "com.example.kafka.KafkaJsonSerializer");
        props.put("acks", "1");
        props.put("batch.size", 256);
        props.put("linger.ms", 5);
        return props;
    }
}
