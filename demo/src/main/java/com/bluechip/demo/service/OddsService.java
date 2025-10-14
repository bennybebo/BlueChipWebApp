package com.bluechip.demo.service;

import com.bluechip.demo.model.Bookmaker;
import com.bluechip.demo.model.Market;
import com.bluechip.demo.model.Odds;
import com.bluechip.demo.model.Outcome;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OddsService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OddsApiService oddsApiService;

    @Value("${data.file-path}")
    private String dataFilePath;

    private static final Map<String, String> SPORT_NAMES = new HashMap<>() {{
        put("americanfootball_nfl", "NFL");
        put("basketball_nba", "NBA");
        put("americanfootball_ncaaf", "NCAAF");
        put("baseball_mlb", "MLB");
        put("basketball_wnba", "WNBA");
        put("icehockey_nhl", "NHL");
        // Add other sports as needed
    }};

    private static final List<Map<String, String>> AVAILABLE_SPORTS = List.of(
        Map.of("key", "americanfootball_nfl", "name", "NFL"),
        Map.of("key", "basketball_nba",      "name", "NBA"),
        Map.of("key", "americanfootball_ncaaf","name","NCAAF"),
        Map.of("key", "baseball_mlb",        "name", "MLB"),
        Map.of("key", "basketball_wnba",     "name", "WNBA"),
        Map.of("key", "icehockey_nhl",       "name", "NHL")
    );

    public List<Odds> getOddsData(String sportKey, String marketType, boolean allowApiRefresh) throws IOException {
        String fileName = "odds_" + sportKey + "_" + marketType + ".json";
        File directory = new File(dataFilePath);
        if (!directory.exists()) {
            directory.mkdirs(); // Create the directory if it doesn't exist
        }
        File file = new File(directory, fileName);

        boolean shouldFetchData = allowApiRefresh || !file.exists();

        if (!file.exists() && !allowApiRefresh) {
            throw new IOException("Cached odds file is missing and API refresh is disabled");
        }

        if (shouldFetchData && allowApiRefresh) {
            // Fetch data from API and save it
            String jsonData = oddsApiService.fetchOddsForSportAndMarket(sportKey, marketType);
            oddsApiService.saveResponseToFile(jsonData, file);
        }

        // Read data from the JSON file
        return objectMapper.readValue(file, new TypeReference<List<Odds>>() {});
    }

    // Helper method to get unique bookmakers offering the specified market type
    public Set<Bookmaker> getUniqueBookmakers(List<Odds> oddsList, String marketType) {
        Map<String, Bookmaker> uniqueBookmakersMap = new HashMap<>();

        for (Odds odds : oddsList) {
            for (Bookmaker bookmaker : odds.getBookmakers()) {
                // Check if the bookmaker offers the specified market type
                boolean offersMarket = false;
                for (Market market : bookmaker.getMarkets()) {
                    if (market.getKey().equals(marketType)) {
                        offersMarket = true;
                        break;  // No need to check further markets once found
                    }
                }

                if (offersMarket) {
                    uniqueBookmakersMap.putIfAbsent(bookmaker.getTitle(), bookmaker);
                }
            }
        }
        return new HashSet<>(uniqueBookmakersMap.values());
    }

    // Helper method to map matchups and bookmakers by market type
    public Map<String, Map<String, Bookmaker>> getMatchupBookmakerMap(List<Odds> oddsList, String marketType) {
        Map<String, Map<String, Bookmaker>> matchupBookmakerMap = new HashMap<>();

        for (Odds odds : oddsList) {
            String matchupKey = odds.getHomeTeam() + " vs " + odds.getAwayTeam();

            // Get or create the bookmakers map for this matchup
            Map<String, Bookmaker> bookmakersForMatchup = matchupBookmakerMap.get(matchupKey);
            if (bookmakersForMatchup == null) {
                bookmakersForMatchup = new HashMap<>();
                matchupBookmakerMap.put(matchupKey, bookmakersForMatchup);
            }

            for (Bookmaker bookmaker : odds.getBookmakers()) {
                // Check if the bookmaker offers the specified market type
                boolean offersMarket = false;
                for (Market market : bookmaker.getMarkets()) {
                    if (market.getKey().equals(marketType)) {
                        offersMarket = true;
                        break;  // No need to check further markets once found
                    }
                }

                if (offersMarket) {
                    bookmakersForMatchup.putIfAbsent(bookmaker.getTitle(), bookmaker);
                }
            }
        }
        return matchupBookmakerMap;
    }

    // Helper method to map sport keys to user-friendly names
    public String getSportName(String sportKey) {
        return SPORT_NAMES.getOrDefault(sportKey, sportKey);
    }

    // Helper method to get a list of available sports
    // TODO: Investigate why this needs to be a list of maps instead of a single map
    public List<Map<String, String>> getAvailableSports() {
        return AVAILABLE_SPORTS;
    }

    // Helper method to determine if a new spread is better than the current best
    public boolean isBetterSpread(Outcome newOutcome, Outcome currentBest, boolean isHomeTeam) {
        if (newOutcome == null) return false;
        if (currentBest == null) return true;

        Double newPoint = newOutcome.getPoint();
        Double currentPoint = currentBest.getPoint();

        // For Home Team: Usually the favorite (negative spread)
        if (isHomeTeam) {
            // For favorites (negative spread), smaller negative number (closer to zero) is better
            if (newPoint < 0 && currentPoint < 0) {
                if (newPoint > currentPoint) return true;
                if (newPoint.equals(currentPoint) && newOutcome.getPrice() > currentBest.getPrice()) return true;
            }
            // If one is negative and the other is positive, choose the positive (better for bettor)
            else if (newPoint >= 0 && currentPoint < 0) {
                return true;
            }
            else if (newPoint >= 0 && currentPoint >= 0) {
                if (newPoint > currentPoint) return true;
                if (newPoint.equals(currentPoint) && newOutcome.getPrice() > currentBest.getPrice()) return true;
            }
        } else {
            // For Away Team: Usually the underdog (positive spread)
            // Larger positive number is better
            if (newPoint > currentPoint) return true;
            if (newPoint.equals(currentPoint) && newOutcome.getPrice() > currentBest.getPrice()) return true;
        }
        return false;
    }

    // Helper method to determine if a new Over total is better than the current best
    public boolean isBetterTotalOver(Outcome newOutcome, Outcome currentBest) {
        if (newOutcome == null) return false;
        if (currentBest == null) return true;

        Double newPoint = newOutcome.getPoint();
        Double currentPoint = currentBest.getPoint();

        // Lower total is better for Over bets
        if (newPoint < currentPoint) return true;
        if (newPoint.equals(currentPoint) && newOutcome.getPrice() > currentBest.getPrice()) return true;

        return false;
    }

    // Helper method to determine if a new Under total is better than the current best
    public boolean isBetterTotalUnder(Outcome newOutcome, Outcome currentBest) {
        if (newOutcome == null) return false;
        if (currentBest == null) return true;

        Double newPoint = newOutcome.getPoint();
        Double currentPoint = currentBest.getPoint();

        // Higher total is better for Under bets
        if (newPoint > currentPoint) return true;
        if (newPoint.equals(currentPoint) && newOutcome.getPrice() > currentBest.getPrice()) return true;

        return false;
    }
}
