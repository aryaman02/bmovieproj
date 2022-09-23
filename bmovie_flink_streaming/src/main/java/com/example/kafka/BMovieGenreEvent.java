package com.example.kafka;

public class BMovieGenreEvent {
    private String imdbID;
    private double userRating;
    private String bMovSeenLocation;
    private String bMovSeenDate;
    private int ticketPrice;
    private int viewerAge;
    private String bMovGenre;

    public BMovieGenreEvent() {

    }

    public BMovieGenreEvent(String imdbID, double userRating, String bMovSeenLocation, String bMovSeenDate,
        int ticketPrice, int viewerAge, String bMovGenre) {
        this.imdbID = imdbID;
        this.userRating = userRating;
        this.bMovSeenLocation = bMovSeenLocation;
        this.bMovSeenDate = bMovSeenDate;
        this.ticketPrice = ticketPrice;
        this.viewerAge = viewerAge;
        this.bMovGenre = bMovGenre;
    }

    public String getImdbID() { return imdbID; }

    public void setImdbID(String imdbID) { this.imdbID = imdbID; }

    public double getUserRating() { return userRating; }

    public void setUserRating(double userRating) { this.userRating = userRating; }

    public String getbMovSeenLocation() { return bMovSeenLocation; }

    public void setbMovSeenLocation(String bMovSeenLocation) { this.bMovSeenLocation = bMovSeenLocation; }

    public String getbMovSeenDate() { return bMovSeenDate; }

    public void setbMovSeenDate(String bMovSeenDate) { this.bMovSeenDate = bMovSeenDate; }

    public int getTicketPrice() { return ticketPrice; }

    public void setTicketPrice(int ticketPrice) { this.ticketPrice = ticketPrice; }

    public int getViewerAge() { return viewerAge; }

    public void setViewerAge(int viewerAge) { this.viewerAge = viewerAge; }

    public String getbMovGenre() { return bMovGenre; }

    public void setbMovGenre(String bMovGenre) { this.bMovGenre = bMovGenre; }

    public String toString() {
        return "IMDB id: " + this.getImdbID() + " Rating: " + this.getUserRating() + " Location: " +
            this.getbMovSeenLocation() + " Price: " + this.getTicketPrice() + " Genre: " + this.getbMovGenre();
    }
}
