package com.example.cassandra;

import com.datastax.driver.core.*;

import com.example.controller.MongoConnectionAdapter;
import com.example.controller.RankingEntry;
import com.example.utils.BMovieConfigProps;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@RestController
public class BMovCassandraController {
    private final ObjectMapper m = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient();
    private Cluster cluster;
    private Session session;
    private static final String CLUSTER_IP_ADDRESS = "127.0.0.1";
    private static final String KEYSPACE = "bmovie_streaming_data";

    private final MongoConnectionAdapter mongoAdapter = new MongoConnectionAdapter();
    private MongoDatabase database;
    private MongoCollection<Document> avgRatingRankingsCol;

    @PostConstruct
    public void initialize() {
        cluster = Cluster.builder().addContactPoint(CLUSTER_IP_ADDRESS).build();
        session = cluster.connect(KEYSPACE);

        String mongoHost = BMovieConfigProps.getMongoDBAddress();
        String mongoDB = System.getProperty("mongodb.database", "ad");
        mongoAdapter.connect(mongoHost, mongoDB);
        database = mongoAdapter.getDatabase();
        avgRatingRankingsCol = database.getCollection("avgRatingRankingsCol");
    }

    @PreDestroy
    public void cleanup() {
        session.close();
        cluster.close();
        mongoAdapter.disconnect();
    }

    @RequestMapping(value="/api/v1/bmovie/general_stats", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> queryCurrentMovieStatsById(@RequestParam(name="id") String imdbID) throws IOException {
        boolean isValidId = true;

        if (imdbID.length() == 9 && imdbID.indexOf("tt") == 0) {
            for (int i = 2; i < imdbID.length(); i++) {
                char ch = imdbID.charAt(i);

                if (!Character.isDigit(ch)) {
                    isValidId = false;
                }
            }
        } else {
            isValidId = false;
        }

        if (!isValidId) {
            return new ResponseEntity<>("Bad Request. Please provide a valid IMDB id.", HttpStatus.BAD_REQUEST);
        }

        String omdbURL = new HttpUrl.Builder() .scheme("https")
            .host("omdbapi.com").addQueryParameter("i", imdbID).addQueryParameter("apikey", "da14bb5c").build().toString();

        Request request = new Request.Builder().url(omdbURL).build();
        Response httpResponse = client.newCall(request).execute();
        String bMovieInfo = httpResponse.body().string();

        httpResponse.close();

        if (bMovieInfo.equals("{\"Response\":\"False\",\"Error\":\"Error getting data.\"}")) {
            return new ResponseEntity<>("Sorry! Could not retrieve the current movie stats for this imdb id.", HttpStatus.NOT_FOUND);
        }
        if (bMovieInfo.indexOf("\"Type\":\"movie\"") == -1) {
            return new ResponseEntity<>("Sorry! Could not retrieve the current movie stats for this imdb id.", HttpStatus.NOT_FOUND);
        }
        int yearIndex = bMovieInfo.indexOf(':', bMovieInfo.indexOf("Year")) + 2;

        StringBuilder sb = new StringBuilder();
        int i = yearIndex;
        char ch = bMovieInfo.charAt(i);

        while (ch != '\"') {
            sb.append(ch);
            i++;
            ch = bMovieInfo.charAt(i);
        }
        int year = Integer.parseInt(sb.toString());

        if (bMovieInfo.indexOf("\"Country\":\"India\"") != -1 && bMovieInfo.indexOf("\"Language\":\"Hindi\"") != -1 && year >= 1990 && year <= 2009) {
            // query on Cassandra db
            String cassandraQuery = "SELECT * FROM general_movie_stats WHERE imdbid = " + "'" + imdbID + "'" + ";";

            // Example search query: SELECT * FROM bmovie_streaming_data.viewcount WHERE imdbid = 'tt0489560';

            ResultSet queriedResult = session.execute(cassandraQuery);

            String id = imdbID;
            long numViews = 0;
            long boxOfficeGross = 0;

            for (Row row : queriedResult) {
                if (!row.isNull(0) && !row.isNull(1) && !row.isNull(2)) {
                    id = row.getString(0);
                    boxOfficeGross = row.getLong(1);
                    numViews = row.getLong(2);
                }
            }
            BMovGeneralStats currMovieStats = new BMovGeneralStats(id, numViews, boxOfficeGross);
            String response = m.writeValueAsString(currMovieStats);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } else {
            return new ResponseEntity<>("Sorry! Could not retrieve the current movie stats for this imdb id.", HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value="/api/v1/bmovie/rank_byrating", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getTopKMoviesByAvgRating(@RequestParam(name="g") String genre)
        throws JsonProcessingException {
        List<Document> query = avgRatingRankingsCol.find(new Document("genre", genre)).into(new ArrayList<>());

        if (query.size() > 0) { // if genre already exists in the collection
            Document doc = query.get(0);
            return new ResponseEntity<>(doc.toJson(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("No results found! Please check that you have entered a valid genre (check the genre "
                + "endpoint for more info) or please check back later for the latest set of rankings.", HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value="/api/v1/bmovie/most_watched_genres", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> queryMostWatchedGenres() throws IOException {
        // query on Cassandra db
        String cassandraQuery = "SELECT * FROM watched_genre_stats;";

        ResultSet queriedResult = session.execute(cassandraQuery);

        List<MostWatchedGenre> mostWatchedGenresInfo = new ArrayList<>();

        for (Row row : queriedResult) {
            if (!row.isNull(0) && !row.isNull(1) && !row.isNull(2)) {
                mostWatchedGenresInfo.add(new MostWatchedGenre(row.getLong(1), row.getString(0), row.getLong(2)));
            }
        }
        if (mostWatchedGenresInfo.size() == 0) {
            return new ResponseEntity<>("No results found! Please check back later to find this information.", HttpStatus.NOT_FOUND);
        }

        String response = m.writeValueAsString(mostWatchedGenresInfo);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
