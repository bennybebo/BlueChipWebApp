package com.bluechip.demo.controller;

import com.bluechip.demo.model.Bookmaker;
import com.bluechip.demo.model.Odds;
import com.bluechip.demo.service.OddsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class OddsController {

    @Autowired
    private OddsService oddsService;

    // H2H Odds Page
    @GetMapping("/odds/h2h")
    public String getH2HOdds(Model model) {
        try {
            List<Odds> oddsList = oddsService.readOddsFromFile();
            
            oddsList.sort(Comparator.comparing(Odds::getCommenceTime));

            // Filter bookmakers that provide H2H odds
            Set<Bookmaker> uniqueBookmakers = getUniqueBookmakers(oddsList, "h2h");
            Map<String, Map<String, Bookmaker>> matchupBookmakerMap = getMatchupBookmakerMap(oddsList, "h2h");

            model.addAttribute("oddsList", oddsList);
            model.addAttribute("uniqueBookmakers", uniqueBookmakers);
            model.addAttribute("matchupBookmakerMap", matchupBookmakerMap);
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Unable to load odds data.");
        }
        return "odds_h2h";
    }

    // Spreads Odds Page
    @GetMapping("/odds/spreads")
    public String getSpreadsOdds(Model model) {
        try {
            List<Odds> oddsList = oddsService.readOddsFromFile();
            
            oddsList.sort(Comparator.comparing(Odds::getCommenceTime));
            
            // Filter bookmakers that provide Spread odds
            Set<Bookmaker> uniqueBookmakers = getUniqueBookmakers(oddsList, "spreads");

            Map<String, Map<String, Bookmaker>> matchupBookmakerMap = getMatchupBookmakerMap(oddsList, "spreads");

            model.addAttribute("oddsList", oddsList);
            model.addAttribute("uniqueBookmakers", uniqueBookmakers);
            model.addAttribute("matchupBookmakerMap", matchupBookmakerMap);
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Unable to load odds data.");
        }
        return "odds_spreads";
    }

    // Helper method to get unique bookmakers by market type
    private Set<Bookmaker> getUniqueBookmakers(List<Odds> oddsList, String marketType) {
        return oddsList.stream()
                .flatMap(odds -> odds.getBookmakers().stream())
                .filter(bookmaker -> bookmaker.getMarkets().stream()
                        .anyMatch(market -> market.getKey().equals(marketType)))
                .collect(Collectors.toMap(Bookmaker::getTitle, bookmaker -> bookmaker, (b1, b2) -> b1))
                .values()
                .stream()
                .collect(Collectors.toSet());
    }

    // Helper method to map matchups and bookmakers by market type
    private Map<String, Map<String, Bookmaker>> getMatchupBookmakerMap(List<Odds> oddsList, String marketType) {
        return oddsList.stream()
                .collect(Collectors.toMap(
                        odds -> odds.getHomeTeam() + " vs " + odds.getAwayTeam(),
                        odds -> odds.getBookmakers().stream()
                                .filter(bookmaker -> bookmaker.getMarkets().stream()
                                        .anyMatch(market -> market.getKey().equals(marketType)))
                                .collect(Collectors.toMap(Bookmaker::getTitle, bookmaker -> bookmaker, (b1, b2) -> b1))
                ));
    }
}
