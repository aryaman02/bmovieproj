package com.example.flink;

public class BMovieRatingsInfo {
    private String imdbID;
    private double currAvgRating;

    public BMovieRatingsInfo(String imdbID, double currAvgRating) {
        this.imdbID = imdbID;
        this.currAvgRating = currAvgRating;
    }

    public String getImdbID() {
        return imdbID;
    }

    public void setImdbID(String imdbID) {
        this.imdbID = imdbID;
    }

    public double getCurrAvgRating() {
        return currAvgRating;
    }

    public void setCurrAvgRating(double currAvgRating) {
        this.currAvgRating = currAvgRating;
    }
}
