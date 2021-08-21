package com.example.generator;

public class BMovieSeenEvent {
    private String imdbID;
    private double userRating;
    private String bMovSeenLocation;
    private String bMovSeenDate;
    private int ticketPrice;
    private int viewerAge;

    public BMovieSeenEvent() {

    }

    public BMovieSeenEvent(String imdbID, double userRating, String bMovSeenLocation, String bMovSeenDate,
        int ticketPrice, int viewerAge) {
        this.imdbID = imdbID;
        this.userRating = userRating;
        this.bMovSeenLocation = bMovSeenLocation;
        this.bMovSeenDate = bMovSeenDate;
        this.ticketPrice = ticketPrice;
        this.viewerAge = viewerAge;
    }

    public void setImdbID(String imdbID) {
        this.imdbID = imdbID;
    }

    public void setUserRating(double userRating) {
        this.userRating = userRating;
    }

    public void setbMovSeenLocation(String bMovSeenLocation) {
        this.bMovSeenLocation = bMovSeenLocation;
    }

    public void setbMovSeenDate(String bMovSeenDate) {
        this.bMovSeenDate = bMovSeenDate;
    }

    public void setTicketPrice(int ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public void setViewerAge(int viewerAge) {
        this.viewerAge = viewerAge;
    }

    public String getImdbID() {
        return imdbID;
    }

    public double getUserRating() {
        return userRating;
    }

    public String getbMovSeenLocation() {
        return bMovSeenLocation;
    }

    public String getbMovSeenDate() {
        return bMovSeenDate;
    }

    public int getTicketPrice() {
        return ticketPrice;
    }

    public int getViewerAge() {
        return viewerAge;
    }

    public String toString() {
        return "IMDB id: " + this.getImdbID() + " Rating: " + this.getUserRating() + " Location: " +
            this.getbMovSeenLocation() + " Price: " + this.getTicketPrice();
    }
}
