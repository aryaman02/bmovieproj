package com.example.flink;

import com.example.kafka.BMovieGenreEvent;
import com.example.utils.BMovieConfigProps;
import com.example.utils.MongoConnectionAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.bson.Document;
import org.bson.conversions.Bson;
import scala.App;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;

public class AggregatorFunctionGenre extends ProcessAllWindowFunction<BMovieGenreEvent, Long, TimeWindow> {
    private final MongoConnectionAdapter mongoAdapter = new MongoConnectionAdapter();
    private MongoDatabase database;
    private ObjectMapper mapper;
    private MongoCollection<Document> avgRatingRankingsCol;
    private AppConfig appConfig;

    public AggregatorFunctionGenre(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void open(Configuration parameters) {
        //String mongoHost = BMovieConfigProps.getMongoDBAddress();
        var mongoHost = appConfig.getProperty("mongoAddress");
        String mongoDB = System.getProperty("mongodb.database", "ad");
        mongoAdapter.connect(mongoHost, mongoDB);
        database = mongoAdapter.getDatabase();
        mapper = mongoAdapter.getJsonMapper();

        if (!database.listCollectionNames().into(new ArrayList()).contains("avgRatingRankingsCol")) {
            database.createCollection("avgRatingRankingsCol");
        }
        avgRatingRankingsCol = database.getCollection("avgRatingRankingsCol");
    }

    @Override
    public void process(Context context, Iterable<BMovieGenreEvent> iterable, Collector<Long> collector)
        throws Exception {
        Map<String, Map<String, Tuple2<Double, Integer>>> avgRatingsMapByGenre = new HashMap<>();

        for (BMovieGenreEvent event : iterable) {
            String genre = event.getbMovGenre();

            if (!avgRatingsMapByGenre.containsKey(genre)) {
                Map<String, Tuple2<Double, Integer>> avgRatingsMap = new HashMap<>();
                avgRatingsMap.put(event.getImdbID(), new Tuple2<Double, Integer>(event.getUserRating(), 1));
                avgRatingsMapByGenre.put(genre, avgRatingsMap);
            } else {
                Map<String, Tuple2<Double, Integer>> avgRatingsMap = avgRatingsMapByGenre.get(genre);
                String imdbID = event.getImdbID();

                if (!avgRatingsMap.containsKey(imdbID)) {
                    avgRatingsMap.put(imdbID, new Tuple2<Double, Integer>(event.getUserRating(), 1));
                } else {
                    Tuple2<Double, Integer> currVals = avgRatingsMap.get(imdbID);
                    avgRatingsMap
                        .put(imdbID, new Tuple2<Double, Integer>(currVals.f0 + event.getUserRating(), currVals.f1 + 1));
                }
                avgRatingsMapByGenre.put(genre, avgRatingsMap);
            }
        }

        for (String genreKey : avgRatingsMapByGenre.keySet()) {
            Map<String, Tuple2<Double, Integer>> aggregatedRatingsMap = avgRatingsMapByGenre.get(genreKey);
            List<BMovieRatingsInfo> topKMovies = new ArrayList<>();

            for (String id : aggregatedRatingsMap.keySet()) {
                double currAvgRating = aggregatedRatingsMap.get(id).f0 / aggregatedRatingsMap.get(id).f1;
                topKMovies.add(new BMovieRatingsInfo(id, currAvgRating));
            }
            Collections.sort(topKMovies, new Comparator<BMovieRatingsInfo>() {
                @Override
                public int compare(BMovieRatingsInfo o1, BMovieRatingsInfo o2) {
                    if (o1.getCurrAvgRating() > o2.getCurrAvgRating()) {
                        return 1;
                    } else if (o1.getCurrAvgRating() < o2.getCurrAvgRating()) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            });
            int k = Math.min(5, topKMovies.size()); // default Top-K is 5
            int numElemsRemoved = topKMovies.size() - k;

            for (int i = 0; i < numElemsRemoved; i++) {
                topKMovies.remove(topKMovies.size() - 1);
            }

            // put the rankings info in MongoDB for each genre
            List<Document> query = avgRatingRankingsCol.find(new Document("genre", genreKey)).into(new ArrayList<>());

            if (query.size() > 0) { // if genre already exists in the collection
                Bson removeDocQuery = eq("genre", genreKey);
                avgRatingRankingsCol.deleteOne(removeDocQuery);
            }
            GenreRatingsInfo rankingInfo = new GenreRatingsInfo(genreKey, context.window().maxTimestamp(), topKMovies);
            String jsonStr = mapper.writeValueAsString(rankingInfo);
            Document doc = Document.parse(jsonStr);
            avgRatingRankingsCol.insertOne(doc);

        }
        collector.collect(context.window().maxTimestamp());
    }

    @Override
    public void close() {
        mongoAdapter.disconnect();
    }
}
