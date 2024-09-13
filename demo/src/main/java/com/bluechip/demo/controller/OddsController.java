package com.bluechip.demo.controller;

import com.bluechip.demo.model.Bookmaker;
import com.bluechip.demo.model.Odds;
import com.bluechip.demo.service.OddsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/odds")
public class OddsController {

    @Autowired
    private OddsService oddsService;

    @GetMapping("/{sport}/{market}")
    public String getOdds(
            @PathVariable("sport") String sportKey,
            @PathVariable("market") String marketType,
            Model model) {
        try {
            // Fetch odds data, which will check for existing JSON file or fetch from API
            List<Odds> oddsList = oddsService.getOddsData(sportKey, marketType);

            // Sort the odds list by commence time
            oddsList.sort(Comparator.comparing(Odds::getCommenceTime));

            // Get unique bookmakers offering the specified market type
            Set<Bookmaker> uniqueBookmakers = getUniqueBookmakers(oddsList, marketType);

            // Map matchups to their corresponding bookmakers and odds
            Map<String, Map<String, Bookmaker>> matchupBookmakerMap = getMatchupBookmakerMap(oddsList, marketType);

            // Map sportKey to a user-friendly sport name
            String sportName = getSportName(sportKey);

            // Get the list of available sports
            List<Map<String, String>> availableSports = getAvailableSports();

            // Add attributes to the model for use in the view
            model.addAttribute("oddsList", oddsList);
            model.addAttribute("uniqueBookmakers", uniqueBookmakers);
            model.addAttribute("matchupBookmakerMap", matchupBookmakerMap);
            model.addAttribute("sportKey", sportKey);
            model.addAttribute("marketType", marketType);
            model.addAttribute("sportName", sportName);
            model.addAttribute("availableSports", availableSports);
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Unable to load odds data.");
        }
        // Return the view corresponding to the market type
        return "odds_" + marketType;
    }

    // Helper method to map sport keys to user-friendly names
    private String getSportName(String sportKey) {
        Map<String, String> sportNames = new HashMap<>();
        sportNames.put("americanfootball_nfl", "NFL");
        sportNames.put("basketball_nba", "NBA");
        sportNames.put("americanfootball_ncaaf", "NCAAF");
        // Add other sports as needed
        return sportNames.getOrDefault(sportKey, sportKey);
    }

    // Helper method to get a list of available sports
    private List<Map<String, String>> getAvailableSports() {
        List<Map<String, String>> availableSports = new ArrayList<>();

        Map<String, String> nfl = new HashMap<>();
        nfl.put("key", "americanfootball_nfl");
        nfl.put("name", "NFL");
        availableSports.add(nfl);

        Map<String, String> nba = new HashMap<>();
        nba.put("key", "basketball_nba");
        nba.put("name", "NBA");
        availableSports.add(nba);

        Map<String, String> ncaaf = new HashMap<>();
        ncaaf.put("key", "americanfootball_ncaaf");
        ncaaf.put("name", "NCAAF");
        availableSports.add(ncaaf);

        // Add more sports as needed

        return availableSports;
    }

    // Helper method to get unique bookmakers offering the specified market type
    private Set<Bookmaker> getUniqueBookmakers(List<Odds> oddsList, String marketType) {
        Map<String, Bookmaker> uniqueBookmakersMap = new HashMap<>();

        for (Odds odds : oddsList) {
            for (Bookmaker bookmaker : odds.getBookmakers()) {
                // Check if the bookmaker offers the specified market type
                boolean offersMarket = bookmaker.getMarkets().stream()
                        .anyMatch(market -> market.getKey().equals(marketType));

                if (offersMarket) {
                    // Add the bookmaker to the map if not already present
                    uniqueBookmakersMap.putIfAbsent(bookmaker.getTitle(), bookmaker);
                }
            }
        }

        // Return a set of unique bookmakers
        return new HashSet<>(uniqueBookmakersMap.values());
    }

    // Helper method to map matchups and bookmakers by market type
    private Map<String, Map<String, Bookmaker>> getMatchupBookmakerMap(List<Odds> oddsList, String marketType) {
        Map<String, Map<String, Bookmaker>> matchupBookmakerMap = new HashMap<>();

        for (Odds odds : oddsList) {
            String matchupKey = odds.getHomeTeam() + " vs " + odds.getAwayTeam();

            // Get or create the bookmakers map for this matchup
            Map<String, Bookmaker> bookmakersForMatchup = matchupBookmakerMap.computeIfAbsent(matchupKey, k -> new HashMap<>());

            for (Bookmaker bookmaker : odds.getBookmakers()) {
                // Check if the bookmaker offers the specified market type
                boolean offersMarket = bookmaker.getMarkets().stream()
                        .anyMatch(market -> market.getKey().equals(marketType));

                if (offersMarket) {
                    // Add the bookmaker to the map if not already present
                    bookmakersForMatchup.putIfAbsent(bookmaker.getTitle(), bookmaker);
                }
            }
        }

        return matchupBookmakerMap;
    }
}

