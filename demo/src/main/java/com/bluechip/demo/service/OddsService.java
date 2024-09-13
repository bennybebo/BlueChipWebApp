package com.bluechip.demo.service;

import com.bluechip.demo.model.Odds;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class OddsService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OddsApiService oddsApiService;

    @Value("${data.file-path}")
    private String dataFilePath;

    public List<Odds> getOddsData(String sportKey, String marketType) throws IOException {
        String fileName = "odds_" + sportKey + "_" + marketType + ".json";
        File directory = new File(dataFilePath);
        if (!directory.exists()) {
            directory.mkdirs(); // Create the directory if it doesn't exist
        }
        File file = new File(directory, fileName);

        // Define data expiration time (e.g., 1 hour)
        long expirationTime = 60 * 60 * 1000; // 1 hour in milliseconds

        boolean shouldFetchData = false;

        if (!file.exists()) {
            shouldFetchData = true;
        } else {
            long lastModified = file.lastModified();
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastModified > expirationTime) {
                shouldFetchData = true;
            }
        }

        if (shouldFetchData) {
            // Fetch data from API and save it
            String jsonData = oddsApiService.fetchOddsForSportAndMarket(sportKey, marketType);
            oddsApiService.saveResponseToFile(jsonData, file);
        }

        // Read data from the JSON file
        return objectMapper.readValue(file, new TypeReference<List<Odds>>() {});
    }
}
