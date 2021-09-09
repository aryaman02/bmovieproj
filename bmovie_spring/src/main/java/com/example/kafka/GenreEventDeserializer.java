package com.example.kafka;

import com.example.generator.BMovieSeenEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class GenreEventDeserializer implements Deserializer {
    @Override
    public void close() {

    }

    @Override
    public void configure(Map map, boolean b) {

    }

    @Override
    public BMovieGenreEvent deserialize(String arg0, byte[] arg1) {
        ObjectMapper mapper = new ObjectMapper();
        BMovieGenreEvent obj = null;
        try {
            obj = mapper.readValue(arg1, BMovieGenreEvent.class);
        } catch (Exception e) {

            e.printStackTrace();
        }
        return obj;
    }
}
