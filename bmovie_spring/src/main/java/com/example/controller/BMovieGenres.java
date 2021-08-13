package com.example.controller;

import java.util.ArrayList;
import java.util.List;

public class BMovieGenres {
    private List<String> genres;

    public BMovieGenres() {
        genres = new ArrayList<>();
    }

    public List<String> getBMovGenres() { return genres; }

    public void addGenre(String genre) { genres.add(genre); }

    public void setGenres(List<String> genres) {this.genres = genres; }
}
