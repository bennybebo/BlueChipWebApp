package com.bluechip.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Setter;
import lombok.Getter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Odds {
    @Getter @Setter
    private String id;

    @JsonProperty("sport_key")  // Map "sport_key" to the Java field "sportKey" e.g.(americanfootball_nfl)
    @Getter @Setter
    private String sportKey;

    @JsonProperty("sport_title")  // Map "sport_title" to the Java field "sportTitle"
    @Getter @Setter
    private String sportTitle;

    @JsonProperty("commence_time")  // Map "commence_time" to the Java field "commenceTime"
    @Getter @Setter
    private String commenceTime;

    @JsonProperty("home_team")  // Map "home_team" to the Java field "homeTeam"
    @Getter @Setter
    private String homeTeam;

    @JsonProperty("away_team")  // Map "away_team" to the Java field "awayTeam"
    @Getter @Setter
    private String awayTeam;

    @Getter @Setter
    private List<Bookmaker> bookmakers;
    
    @Getter @Setter
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

}
