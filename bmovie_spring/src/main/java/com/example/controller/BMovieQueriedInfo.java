package com.example.controller;

import java.util.ArrayList;
import java.util.List;

public class BMovieQueriedInfo {
    private boolean isValid;
    private List<String> imdbIDs;

    public BMovieQueriedInfo(boolean flag) {
        imdbIDs = new ArrayList<>();
        isValid = flag;
    }
    public List<String> getQueriedResults() {
        return imdbIDs;
    }

    public void setImdbIDs(List<String> imdbIDs) {
        this.imdbIDs = imdbIDs;
    }

    public void addImdbID(String id) {
        imdbIDs.add(id);
    }

    public void setFlag(boolean flag) { isValid = flag; }

    public boolean getQueryStatus() { return isValid; }
}
