package com.example.flink;

import com.example.generator.BMovieSeenEvent;
import com.example.kafka.BMovieGenreEvent;
import com.example.utils.BMovieConfigProps;
import com.example.utils.MongoConnectionAdapter;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class BMovieSeenEventEnricher extends RichFlatMapFunction<BMovieSeenEvent, BMovieGenreEvent> {
    private AppConfig appConfig;
    private final MongoConnectionAdapter mongoAdapter = new MongoConnectionAdapter();
    private MongoDatabase database;
    private MongoCollection<Document> bMovieDataCol;

    public BMovieSeenEventEnricher(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void open(Configuration config) {
        //String mongoHost = BMovieConfigProps.getMongoDBAddress();
        var mongoHost = appConfig.getProperty("mongoAddress");
        String mongoDB = System.getProperty("mongodb.database", "ad");
        mongoAdapter.connect(mongoHost, mongoDB);
        database = mongoAdapter.getDatabase();
        bMovieDataCol = database.getCollection("bMovieDataCol");
    }

    @Override
    public void flatMap(BMovieSeenEvent event, Collector<BMovieGenreEvent> collector) throws Exception {
        // we are already working with good bmovieseenevents

        List<String> genres = (List<String>) bMovieDataCol.find(new Document("imdbID", event.getImdbID())).into(new ArrayList<>()).get(0).get("genres");

        for (String genre : genres) {
            BMovieGenreEvent genreEvent = new BMovieGenreEvent(event.getImdbID(), event.getUserRating(), event.getbMovSeenLocation(),
                event.getbMovSeenDate(), event.getTicketPrice(), event.getViewerAge(), genre);

            collector.collect(genreEvent); // pushing genreEvent into the output datastream
        }
    }

    @Override
    public void close() throws Exception {
        mongoAdapter.disconnect();
    }
}
