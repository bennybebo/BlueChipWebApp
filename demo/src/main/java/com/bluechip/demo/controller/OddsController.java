package com.bluechip.demo.controller;

import com.bluechip.demo.model.Bookmaker;
import com.bluechip.demo.model.Odds;
import com.bluechip.demo.service.OddsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class OddsController {

    @Autowired
    private OddsService oddsService;

    @GetMapping("/odds")
    public String getOdds(Model model) {
        try {
            List<Odds> oddsList = oddsService.readOddsFromFile();
            
            // Extract unique bookmakers (no duplicates across matchups)
            Set<Bookmaker> uniqueBookmakers = oddsList.stream()
                    .flatMap(odds -> odds.getBookmakers().stream())
                    .collect(Collectors.toMap(Bookmaker::getTitle, bookmaker -> bookmaker, (b1, b2) -> b1))
                    .values()
                    .stream()
                    .collect(Collectors.toSet());

            // Create a map of matchups and their associated bookmakers (handling duplicates by picking one entry)
            Map<String, Map<String, Bookmaker>> matchupBookmakerMap = oddsList.stream()
                .collect(Collectors.toMap(
                    odds -> odds.getHomeTeam() + " vs " + odds.getAwayTeam(),
                    odds -> odds.getBookmakers().stream()
                            .collect(Collectors.toMap(Bookmaker::getTitle, bookmaker -> bookmaker, (b1, b2) -> b1)) // Deduplicate by bookmaker title
                ));

            model.addAttribute("oddsList", oddsList);
            model.addAttribute("uniqueBookmakers", uniqueBookmakers);  // Only unique bookmakers for the table header
            model.addAttribute("matchupBookmakerMap", matchupBookmakerMap); // Map for bookmaker odds
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Unable to load odds data.");
        }
        return "odds";
    }
}
