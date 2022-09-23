package com.example.flink;

import java.util.List;

public class GenreRatingsInfo {
    private String genre;
    private long timestamp;
    private List<BMovieRatingsInfo> ranking;

    public GenreRatingsInfo(String genre, long timestamp, List<BMovieRatingsInfo> ranking) {
        this.genre = genre;
        this.timestamp = timestamp;
        this.ranking = ranking;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<BMovieRatingsInfo> getRanking() {
        return ranking;
    }

    public void setRanking(List<BMovieRatingsInfo> ranking) {
        this.ranking = ranking;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }
}
