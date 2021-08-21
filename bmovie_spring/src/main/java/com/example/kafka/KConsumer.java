package com.example.kafka;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Serdes;

public class KConsumer implements ConsumerRebalanceListener, Runnable {
    private static final int POLL_WAIT_TIME = 1000;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private KafkaConsumer<String, String> consumer;
    private String clientId;

    public void initialize() {
        clientId =  "Demo-Consumer-" + UUID.randomUUID().toString();
        Properties props = createProps();
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("demo_topic"), this);
    }

    public void stopConsumer() {
        running.set(false);
        consumer.wakeup();
    }

    private Properties createProps() {
        Properties props = new Properties();
        String kafkaConnectString = "0.0.0.0:9092";

        props.put("bootstrap.servers", kafkaConnectString);
        props.put("key.deserializer", Serdes.String().deserializer().getClass().getName());
        props.put("value.deserializer", Serdes.String().deserializer().getClass().getName());

        props.put("group.id", "stock_processor");
        props.put("application.id", "stock_processor");
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
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(POLL_WAIT_TIME));
                // 2. Get message
                if (records == null) {
                    continue;
                }

                // 3. consume and process (record count will be 1, because max.poll.records = 1
                for (ConsumerRecord<String, String> record : records) {
                    //System.out.println("Consumer with ClientId: " + clientId + " read message: " + record.value() + "  from partition: " + record.partition());

                    // 4. Commit (ack) -> increment the offset in Kafka
                    consumer.commitAsync();
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
        consumer.close();
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        partitions.forEach(partition -> System.out.println("For consumer: " + clientId + " Partition revoked: " + partition.partition()));

        for (TopicPartition tp : partitions) {
            //tp.
            //tp.
        }
        consumer.commitSync();

    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        /*for (TopicPartition tp : partitions) {
            consumer.seek(tp, 30);
        }*/

        partitions.forEach(partition -> System.out.println("For consumer: " + clientId + " Partition assigned: " + partition.partition()));
    }
}
