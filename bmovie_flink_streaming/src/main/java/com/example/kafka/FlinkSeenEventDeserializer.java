package com.example.kafka;

import com.example.generator.BMovieSeenEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class FlinkSeenEventDeserializer implements KafkaDeserializationSchema<BMovieSeenEvent> {
    private final ObjectMapper mapper = new ObjectMapper();
    @Override
    public boolean isEndOfStream(BMovieSeenEvent event) {
        return false;
    }

    @Override
    public BMovieSeenEvent deserialize(ConsumerRecord<byte[], byte[]> consumerRecord) throws Exception {
        BMovieSeenEvent obj = null;
        try {
            obj = mapper.readValue(consumerRecord.value(), BMovieSeenEvent.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

    @Override
    public TypeInformation<BMovieSeenEvent> getProducedType() {
        return TypeInformation.of(BMovieSeenEvent.class);
    }
}
