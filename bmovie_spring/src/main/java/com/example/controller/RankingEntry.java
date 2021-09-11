package com.example.controller;

import java.util.ArrayList;
import java.util.List;

public class RankingEntry {
    private String imdbID;
    private double val;

    public RankingEntry(String id, double v) {
        imdbID = id;
        val = v;
    }

    public String getImdbID() {
        return imdbID;
    }

    public void setImdbID(String imdbID) {
        this.imdbID = imdbID;
    }

    public double getVal() {
        return val;
    }

    public void setVal(double val) {
        this.val = val;
    }
}
