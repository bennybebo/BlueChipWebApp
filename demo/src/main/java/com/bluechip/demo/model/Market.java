package com.bluechip.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class Market {
    private String key; // e.g. "totals", "spreads", "h2h"

    @JsonProperty("last_update")
    private Instant lastUpdate; 

    private List<Outcome> outcomes;

}