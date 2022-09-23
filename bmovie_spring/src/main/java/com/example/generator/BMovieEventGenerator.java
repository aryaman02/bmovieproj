package com.example.generator;

import com.example.controller.MongoConnectionAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoDatabase;
import okhttp3.*;
import org.apache.kafka.common.protocol.types.Field;
import org.bson.Document;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BMovieEventGenerator {
    private final MongoConnectionAdapter mongoConnectionAdapter = new MongoConnectionAdapter();
    private MongoDatabase database;
    private OkHttpClient client = new OkHttpClient();
    private final ObjectMapper m = new ObjectMapper();

    private List<String> bMovieIMDBids = new ArrayList<>();
    private final List<String> INDIAN_STATES = Arrays.asList("AN", "AP", "AR", "AS", "BR", "CH", "CT", "DN", "DD", "DL", "GA", "GJ", "HR",
        "HP", "JK", "JH", "KA", "KL", "LD", "MP", "MH", "MN", "ML", "MZ", "NL", "OR", "PY", "PB", "RJ", "SK", "TN", "TG", "TR", "UP", "UT", "WB");

    private static final int BATCH_REQUEST_SIZE = 10;
    private final LinkedBlockingQueue<BMovieSeenEvent> lbq = new LinkedBlockingQueue<>();
    private List<BMovieSeenEvent> events = new ArrayList<>();
    private static final String POST_REQUEST_URL = "http://0.0.0.0:8080/api/v1/bmovie/publish";

    public BMovieEventGenerator() {
        connectToMongoDB();
        populateIMDBidsList();
    }

    private void connectToMongoDB() {
        String mongoHost = System.getProperty("mongodb.host", "0.0.0.0");
        String mongoDB = System.getProperty("mongodb.database", "ad");
        mongoConnectionAdapter.connect(mongoHost, mongoDB);
        database = mongoConnectionAdapter.getDatabase();
    }

    public void terminateMongoConnection() {
        mongoConnectionAdapter.disconnect();
    }

    private void populateIMDBidsList() {
        List<Document> bMoviesList = database.getCollection("bMovieDataCol").find().into(new ArrayList<>());

        for (Document doc : bMoviesList) {
            bMovieIMDBids.add(doc.getString("imdbID"));
        }
    }

    public void generateBMovieSeenEvents() throws InterruptedException {
        while (true) {
            int badEventCandidate = (int) (Math.random() * 10);

            for (int i = 0; i < 10; i++) {
                if (i == badEventCandidate) {
                    BMovieSeenEvent bMovieBadEvent = generateBadEvent();
                    System.out.println(bMovieBadEvent); // for testing purposes
                    lbq.add(bMovieBadEvent);
                } else {
                    BMovieSeenEvent bMovieGoodEvent = generateGoodEvent();
                    System.out.println(bMovieGoodEvent); // for testing purposes
                    lbq.add(bMovieGoodEvent);
                }
                Thread.sleep((long) (Math.random() * 4970 + 30));// sleep after generating bmovieseen event
            }
        }
    }

    /*public void generateBMovieSeenFiniteEvents() throws InterruptedException {
        int badEventCandidate = (int) (Math.random() * 10);

        for (int i = 0; i < 10; i++) {
            if (i == badEventCandidate) {
                BMovieSeenEvent bMovieBadEvent = generateBadEvent();
                //System.out.println(bMovieBadEvent); // for testing purposes
                lbq.add(bMovieBadEvent);
            } else {
                BMovieSeenEvent bMovieGoodEvent = generateGoodEvent();
                //System.out.println(bMovieGoodEvent); // for testing purposes
                lbq.add(bMovieGoodEvent);
            }
            //Thread.sleep((long) (Math.random() * 4970 + 30));// sleep after generating bmovieseen event
        }
    }*/

    private BMovieSeenEvent generateGoodEvent() {
        String randIMDBid = bMovieIMDBids.get((int) (Math.random() * bMovieIMDBids.size()));

        double userRating = Math.random() * 11;
        DecimalFormat dformat = new DecimalFormat("#.#");
        userRating = Double.parseDouble(dformat.format(userRating));

        while (userRating > 10.0) {
            userRating = Math.random() * 11;
            userRating = Double.parseDouble(dformat.format(userRating));
        }

        String state = INDIAN_STATES.get((int) (Math.random() * INDIAN_STATES.size()));

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        String bMovSeenDate = formatter.format(date);

        int bMovTicketPrice = (int) (Math.random() * 151) + 30;

        int viewerAge = (int) (Math.random() * 120) + 1;

        return new BMovieSeenEvent(randIMDBid, userRating, state, bMovSeenDate, bMovTicketPrice, viewerAge);
    }

    private BMovieSeenEvent generateBadEvent() {
        List<String> powerSet = Arrays.asList("001", "010", "011", "100", "101", "110", "111");

        int randIndex = (int) (Math.random() * powerSet.size());
        BMovieSeenEvent event = generateGoodEvent();

        switch (randIndex) {
        case 0:
            event.setTicketPrice(generateBadTicketPrice());
            break;
        case 1:
            event.setbMovSeenLocation(generateBadLocation());
            break;
        case 2:
            event.setbMovSeenLocation(generateBadLocation());
            event.setTicketPrice(generateBadTicketPrice());
            break;
        case 3:
            event.setUserRating(generateBadUserRating());
            break;
        case 4:
            event.setUserRating(generateBadUserRating());
            event.setTicketPrice(generateBadTicketPrice());
            break;
        case 5:
            event.setUserRating(generateBadUserRating());
            event.setbMovSeenLocation(generateBadLocation());
            break;
        default:
            event.setUserRating(generateBadUserRating());
            event.setbMovSeenLocation(generateBadLocation());
            event.setTicketPrice(generateBadTicketPrice());
        }
        // return malformed bmovieseen event
        return event;
    }

    private double generateBadUserRating() {
        double userRating = Math.random() * 2.0e9 - 1.0e9;
        DecimalFormat dformat = new DecimalFormat("#.#");
        userRating = Double.parseDouble(dformat.format(userRating));

        while (userRating >= 0 && userRating <= 10.0) {
            userRating = Math.random() * 2.0e9 - 1.0e9;
            userRating = Double.parseDouble(dformat.format(userRating));
        }
        return userRating;
    }

    private String generateBadLocation() {
        String badString = generateRandomString();

        while (INDIAN_STATES.contains(badString)) {
            badString = generateRandomString();
        }
        return badString;
    }

    private String generateRandomString() {
        // length is bounded by 256 Character
        int len = (int) (Math.random() * 256);
        byte[] array = new byte[256];
        new Random().nextBytes(array);

        String randomString
            = new String(array, Charset.forName("UTF-8"));

        // Create a StringBuffer to store the result
        StringBuilder sb = new StringBuilder();

        // Append first n alphanumeric characters
        // from the generated random String into the result
        for (int k = 0; k < randomString.length(); k++) {

            char ch = randomString.charAt(k);

            if (((ch >= 'a' && ch <= 'z')
                || (ch >= 'A' && ch <= 'Z')
                || (ch >= '0' && ch <= '9'))
                && (len > 0)) {

                sb.append(ch);
                len--;
            }
        }
        // return the resultant string
        return sb.toString();
    }

    private int generateBadTicketPrice() {
        int ticketPrice = (int) (Math.random() * 2000000) - 1000000;

        while (ticketPrice >= 30 && ticketPrice <= 180) {
            ticketPrice = (int) (Math.random() * 2000000) - 1000000;
        }
        return ticketPrice;
    }

    public void assembleANDSubmitBMovSeenEvents() throws IOException {
        while (events.size() < BATCH_REQUEST_SIZE) {
            try {
                BMovieSeenEvent bMovSeenEvent = lbq.poll(1000, TimeUnit.MILLISECONDS);

                if (bMovSeenEvent == null) {
                    continue;
                }
                events.add(bMovSeenEvent);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String jsonStr = m.writeValueAsString(events);

        // make HTTP POST Request
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), jsonStr);

        Request request = new Request.Builder().url(POST_REQUEST_URL).post(body).build(); // make the POST request
        Response response = client.newCall(request).execute(); // send the POST Request to endpoint

        //System.out.println(response.body().string()); // message sent back to client to confirm that bmovie_seen events successfully published

        response.close();

        events.clear(); // empty your batch of bmovie_seen events
    }

    private class Worker implements Runnable {
        private BMovieEventGenerator generator;

        public Worker(BMovieEventGenerator generator) {
            this.generator = generator;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    generator.assembleANDSubmitBMovSeenEvents();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void startWorkerThread() {
        new Thread(new Worker(this)).start();
    }
}
