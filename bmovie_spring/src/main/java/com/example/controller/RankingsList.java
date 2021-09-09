package com.example.controller;

import java.util.ArrayList;
import java.util.List;

public class RankingsList {
    private String topKStatus;
    private List<String> imdbIDs;

    public RankingsList(String str) {
        imdbIDs = new ArrayList<>();
        topKStatus = str;
    }
    public List<String> getRankingsList() {
        return imdbIDs;
    }

    public void setImdbIDs(List<String> imdbIDs) {
        this.imdbIDs = imdbIDs;
    }

    public void addImdbID(String id) {
        imdbIDs.add(id);
    }

    public void setTopKStatus(String status) { topKStatus = status; }

    public String getTopKStatus() { return topKStatus; }
}
