package com.example.flink;

import com.example.generator.BMovieSeenEvent;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

public class AggregatorFunctionId extends KeyedProcessFunction<String, BMovieSeenEvent, Tuple3<Long, Long, String>> {
    private ValueState<MovieStatsCounter> state;

    @Override
    public void open(Configuration parameters) throws Exception {
        state = getRuntimeContext().getState(new ValueStateDescriptor<>("MovieStatsState", MovieStatsCounter.class));
    }

    @Override
    public void processElement(BMovieSeenEvent event, Context context, Collector<Tuple3<Long, Long, String>> collector)
        throws Exception {

        MovieStatsCounter current = state.value();
        if (current == null) { // if we come across a new imdbID (no views)
            current = new MovieStatsCounter();
            current.imdbID = event.getImdbID();
        }

        // update the state - aggregate collection earnings of movie and its viewcount
        current.viewCount++;
        long earnings = event.getTicketPrice();
        current.collectionEarnings += earnings;

        // put the results into the output datastream
        collector.collect(new Tuple3<Long, Long, String>(current.viewCount, current.collectionEarnings, current.imdbID));

        // write the state back
        state.update(current);
    }

    static class MovieStatsCounter {
        String imdbID;
        long viewCount;
        long collectionEarnings;
    }
}
