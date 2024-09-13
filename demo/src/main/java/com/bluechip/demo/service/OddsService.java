package com.bluechip.demo.service;

import com.bluechip.demo.model.Odds;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class OddsService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OddsApiService oddsApiService;

    public List<Odds> getOddsData(String sportKey, String marketType) throws IOException {
        String fileName = "odds_" + sportKey + "_" + marketType + ".json";
        File file = new File(fileName);

        // Check if the file exists
        if (!file.exists()) {
            // Fetch data from API and save it
            String jsonData = oddsApiService.fetchOddsForSportAndMarket(sportKey, marketType);
            oddsApiService.saveResponseToFile(jsonData, fileName);
        }

        // Read data from the JSON file
        return objectMapper.readValue(file, new TypeReference<List<Odds>>() {});
    }
}
