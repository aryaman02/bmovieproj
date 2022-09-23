package com.example.cassandra;

public class MostWatchedGenre {
    private long timestamp;
    private String mostWatchedGenre;
    private long maxCount;

    public MostWatchedGenre(long timestamp, String mostWatchedGenre, long maxCount) {
        this.timestamp = timestamp;
        this.mostWatchedGenre = mostWatchedGenre;
        this.maxCount = maxCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMostWatchedGenre() {
        return mostWatchedGenre;
    }

    public void setMostWatchedGenre(String mostWatchedGenre) {
        this.mostWatchedGenre = mostWatchedGenre;
    }

    public long getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(long maxCount) {
        this.maxCount = maxCount;
    }
}
