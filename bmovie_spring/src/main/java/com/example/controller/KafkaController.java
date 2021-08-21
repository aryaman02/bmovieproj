package com.example.controller;

import com.example.generator.BMovieSeenEvent;
import com.example.kafka.BMovieSeenEventsPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

@RestController
public class KafkaController {
    @Autowired
    private final BMovieSeenEventsPublisher publisher = new BMovieSeenEventsPublisher();

    private static final int NUM_PARTITIONS = 12;

    @PostConstruct
    public void initialize() {
        publisher.initialize();
    }

    @PreDestroy
    public void cleanup() {
        publisher.cleanup();
    }

    @RequestMapping(value="/api/v1/bmovie/publish", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> publishBMovieEventsToKafka(@RequestBody List<BMovieSeenEvent> events) {
        for (BMovieSeenEvent event : events) {
            publisher.publishBMovEvent(event.getImdbID(), event, (int) (Math.random() * NUM_PARTITIONS));
        }
        return new ResponseEntity<>("Successfully added bmovie_seen events to Kafka!", HttpStatus.OK);
    }
}
