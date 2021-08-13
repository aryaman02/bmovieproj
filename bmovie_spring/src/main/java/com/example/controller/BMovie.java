package com.example.controller;

import java.util.List;

public class BMovie {
    private String imdbID;
    private String title;
    private int yearOfRelease;
    private int duration;
    private List<String> genres;

    private List<String> actors;
    private double imdbRating;
    private String description;

    public BMovie() {

    }

    public BMovie(String imdbID) {
        this.imdbID = imdbID;
    }

    public BMovie(String imdbID, String title, int yearOfRelease, int duration, List<String> genres, List<String> actors, double imdbRating, String description) {
        this.imdbID = imdbID;
        this.title = title;
        this.yearOfRelease = yearOfRelease;
        this.duration = duration;
        this.genres = genres;
        this.actors = actors;
        this.imdbRating = imdbRating;
        this.description = description;
    }

    public String getImdbID() {
        return imdbID;
    }

    public void setImdbID(String imdbID) {
        this.imdbID = imdbID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYearOfRelease() {
        return yearOfRelease;
    }

    public void setYearOfRelease(int yearOfRelease) {
        this.yearOfRelease = yearOfRelease;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public List<String> getActors() {
        return actors;
    }

    public void setActors(List<String> actors) {
        this.actors = actors;
    }

    public double getImdbRating() {
        return imdbRating;
    }

    public void setImdbRating(double imdbRating) {
        this.imdbRating = imdbRating;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
