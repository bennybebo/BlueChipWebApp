package com.bluechip.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class Bookmaker {
    private String key;    // e.g. "draftkings"
    private String title;  // e.g. "DraftKings"

    @JsonProperty("last_update")
    private Instant lastUpdate;

    private List<Market> markets;

}
