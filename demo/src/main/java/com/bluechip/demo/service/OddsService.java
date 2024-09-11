package com.bluechip.demo.service;

import com.bluechip.demo.model.Odds;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class OddsService {

    public List<Odds> readOddsFromFile() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File("src/main/resources/odds.json");

        // Map JSON data to a list of Odds objects
        List<Odds> oddsList = objectMapper.readValue(file, new TypeReference<List<Odds>>() {});
        
        return oddsList;
    }
}
