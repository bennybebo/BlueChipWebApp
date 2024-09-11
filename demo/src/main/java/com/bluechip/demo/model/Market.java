package com.bluechip.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Market {
    private String key;
    
    @JsonProperty("last_update")
    private String lastUpdate;
    private List<Outcome> outcomes;

    // Getters and Setters
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public List<Outcome> getOutcomes() {
        return outcomes;
    }

    public void setOutcomes(List<Outcome> outcomes) {
        this.outcomes = outcomes;
    }
}
