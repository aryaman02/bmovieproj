package com.example.kafka;

import com.example.generator.BMovieSeenEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class SeenEventDeserializer implements Deserializer {
    @Override
    public void close() {

    }

    @Override
    public void configure(Map map, boolean b) {

    }

    @Override
    public BMovieSeenEvent deserialize(String arg0, byte[] arg1) {
        ObjectMapper mapper = new ObjectMapper();
        BMovieSeenEvent obj = null;
        try {
            obj = mapper.readValue(arg1, BMovieSeenEvent.class);
        } catch (Exception e) {

            e.printStackTrace();
        }
        return obj;
    }
}
