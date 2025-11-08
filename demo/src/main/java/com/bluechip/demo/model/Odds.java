package com.bluechip.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Setter
public class Odds {
    private String id;

    @JsonProperty("sport_key")
    private String sportKey;

    @JsonProperty("sport_title")
    private String sportTitle;

    @JsonProperty("commence_time")
    private Instant commenceTime;

    @JsonProperty("home_team")
    private String homeTeam;

    @JsonProperty("away_team")
    private String awayTeam;

    private List<Bookmaker> bookmakers;

    private BestOdds bestOdds;

    private FairPrices fairPrices;

    // ---- Convenience formatting for your templates ----
    private static final ZoneId UI_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter UI_FMT = DateTimeFormatter.ofPattern("M/d - h:mm a");

    public ZonedDateTime getCommenceTimeAsZonedDateTime() {
        return commenceTime == null ? null : commenceTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(UI_ZONE);
    }

    public String getFormattedCommenceTime() {
        ZonedDateTime zdt = getCommenceTimeAsZonedDateTime();
        return zdt == null ? "" : zdt.format(UI_FMT);
    }
}

