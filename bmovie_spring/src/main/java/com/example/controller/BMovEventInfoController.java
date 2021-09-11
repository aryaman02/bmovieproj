package com.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

@RestController
public class BMovEventInfoController {
    @Autowired
    private final BMovEventInfoService service = new BMovEventInfoService();
    private static final String REDIS_HOST = "0.0.0.0";
    private static final int REDIS_PORT = 6379;
    private final ObjectMapper m = new ObjectMapper();

    @PostConstruct
    public void initialize() {
        service.initializePool(REDIS_HOST, REDIS_PORT);
    }

    @PreDestroy
    public void cleanup() {
        service.closePool();
    }

    @RequestMapping(value="/api/v1/bmovie/rank_byrating", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getTopKMoviesByUserRating(@RequestParam(name="g") String genre, @RequestParam(name="k") String num)
        throws JsonProcessingException {
        int k = -1;
        boolean isValidNum = false;
        try {
            k = Integer.parseInt(num);
            isValidNum = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse the integer k: " + ex);
        }

        if (isValidNum && k <= 0) {
            isValidNum = false;
        }
        if (!isValidNum) {
            return new ResponseEntity<>("Bad Request - Please enter an integer k with value > 0.", HttpStatus.BAD_REQUEST);
        }

        List<RankingEntry> listOfRankings = service.handleUserRatingRequest(genre, k);

        if (listOfRankings.size() == 0) {
            return new ResponseEntity<>("No results found! Please check that you have entered a valid genre (check the genre "
                + "endpoint for more info) or please check back later for the latest set of rankings.", HttpStatus.NOT_FOUND);
        }

        String response = m.writeValueAsString(listOfRankings);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value="/api/v1/bmovie/rank_bygross", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getTopKMoviesByGrossEarnings(@RequestParam(name="k") String num)
        throws JsonProcessingException {
        int k = -1;
        boolean isValidNum = false;
        try {
            k = Integer.parseInt(num);
            isValidNum = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse the integer k: " + ex);
        }

        if (isValidNum && k <= 0) {
            isValidNum = false;
        }
        if (!isValidNum) {
            return new ResponseEntity<>("Bad Request - Please enter an integer k with value > 0.", HttpStatus.BAD_REQUEST);
        }

        List<RankingEntry> listOfRankings = service.handleGrossEarningsRequest(k);

        if (listOfRankings.size() == 0) {
            return new ResponseEntity<>("No results found! Please check back later for the latest set of rankings.", HttpStatus.NOT_FOUND);
        }

        String response = m.writeValueAsString(listOfRankings);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value="/api/v1/bmovie/rank_byviews", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getTopKMoviesByTotalViews(@RequestParam(name="k") String num)
        throws JsonProcessingException {
        int k = -1;
        boolean isValidNum = false;
        try {
            k = Integer.parseInt(num);
            isValidNum = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse the integer k: " + ex);
        }

        if (isValidNum && k <= 0) {
            isValidNum = false;
        }
        if (!isValidNum) {
            return new ResponseEntity<>("Bad Request - Please enter an integer k with value > 0.", HttpStatus.BAD_REQUEST);
        }

        List<RankingEntry> listOfRankings = service.handleViewerShipRequest(k);

        if (listOfRankings.size() == 0) {
            return new ResponseEntity<>("No results found! Please check back later for the latest set of rankings.", HttpStatus.NOT_FOUND);
        }

        String response = m.writeValueAsString(listOfRankings);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
