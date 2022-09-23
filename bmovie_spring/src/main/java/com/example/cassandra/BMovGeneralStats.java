package com.example.cassandra;

public class BMovGeneralStats {
    private String imdbID;
    private long numViews;
    private long collectionEarnings;

    public BMovGeneralStats() {

    }

    public BMovGeneralStats(String imdbID) {
        this.imdbID = imdbID;
    }

    public BMovGeneralStats(String imdbID, long views, long earnings) {
        this.imdbID = imdbID;
        this.numViews = views;
        this.collectionEarnings = earnings;
    }

    public String getImdbID() { return imdbID; }


    public void setImdbID(String imdbID) {
        this.imdbID = imdbID;
    }

    public long getNumViews() { return numViews; }

    public void setNumViews(long numViews) {
        this.numViews = numViews;
    }

    public long getCollectionEarnings() { return collectionEarnings; }

    public void setCollectionEarnings(long earnings) {
        this.collectionEarnings = earnings;
    }
}
