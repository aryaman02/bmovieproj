package com.example.flink;

import com.example.kafka.BMovieGenreEvent;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.HashMap;
import java.util.Map;

public class GenreWatchedAggregatorFunction implements
    AllWindowFunction<BMovieGenreEvent, Tuple3<String, Long, Long>, TimeWindow> {

    @Override
    public void apply(TimeWindow timeWindow, Iterable<BMovieGenreEvent> iterable,
        Collector<Tuple3<String, Long, Long>> collector) throws Exception {
        Map<String, Long> freqGenreMap = new HashMap<>();

        for (BMovieGenreEvent event : iterable) {
            if (!freqGenreMap.containsKey(event.getbMovGenre())) {
                freqGenreMap.put(event.getbMovGenre(), 1L);
            } else {
                freqGenreMap.put(event.getbMovGenre(), freqGenreMap.get(event.getbMovGenre())+1L);
            }
        }
        long maxCount = -1;
        String mostWatchedGenre = "";

        for (String key : freqGenreMap.keySet()) {
            if (maxCount < freqGenreMap.get(key)) {
                maxCount = freqGenreMap.get(key);
                mostWatchedGenre = key;
            }
        }
        collector.collect(new Tuple3<String, Long, Long>(mostWatchedGenre, timeWindow.maxTimestamp(), maxCount));
    }
}
