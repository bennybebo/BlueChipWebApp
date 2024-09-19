package com.bluechip.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Odds {

    private String id;

    @JsonProperty("sport_key")  // Map "sport_key" to the Java field "sportKey" e.g.(americanfootball_nfl)
    private String sportKey;

    @JsonProperty("sport_title")  // Map "sport_title" to the Java field "sportTitle"
    private String sportTitle;

    @JsonProperty("commence_time")  // Map "commence_time" to the Java field "commenceTime"
    private String commenceTime;

    @JsonProperty("home_team")  // Map "home_team" to the Java field "homeTeam"
    private String homeTeam;

    @JsonProperty("away_team")  // Map "away_team" to the Java field "awayTeam"
    private String awayTeam;

    private List<Bookmaker> bookmakers;
    
    private BestOdds bestOdds;

    // Method to parse commenceTime string into ZonedDateTime
    public ZonedDateTime getCommenceTimeAsZonedDateTime() {
        try {
            // Assuming commenceTime is in ISO 8601 format (e.g., "2024-09-22T17:01:00Z")
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(commenceTime);
            // Convert the time to Eastern Standard Time (EST)
            return zonedDateTime.withZoneSameInstant(ZoneId.of("America/New_York"));
        } catch (Exception e) {
            // Handle parsing error, return null or log error
            return null;
        }
    }

    // Method to return formatted commenceTime for display
    public String getFormattedCommenceTime() {
        ZonedDateTime zonedDateTime = getCommenceTimeAsZonedDateTime();
        if (zonedDateTime != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d - h:mm a");
            return zonedDateTime.format(formatter);
        }
        return "";
    }

    
    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSportKey() {
        return sportKey;
    }

    public void setSportKey(String sportKey) {
        this.sportKey = sportKey;
    }

    public String getSportTitle() {
        return sportTitle;
    }

    public void setSportTitle(String sportTitle) {
        this.sportTitle = sportTitle;
    }

    public String getCommenceTime() {
        return commenceTime;
    }

    public void setCommenceTime(String commenceTime) {
        this.commenceTime = commenceTime;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(String homeTeam) {
        this.homeTeam = homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(String awayTeam) {
        this.awayTeam = awayTeam;
    }

    public List<Bookmaker> getBookmakers() {
        return bookmakers;
    }

    public void setBookmakers(List<Bookmaker> bookmakers) {
        this.bookmakers = bookmakers;
    }

	public BestOdds getBestOdds() {
		return bestOdds;
	}

	public void setBestOdds(BestOdds bestOdds) {
		this.bestOdds = bestOdds;
	}
}
