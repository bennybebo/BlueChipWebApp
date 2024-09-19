package com.bluechip.demo.controller;

import com.bluechip.demo.model.*;
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

            // Compute best odds for each Odds object
            computeBestOdds(oddsList);

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

    // New method to compute best odds for each Odds object
    private void computeBestOdds(List<Odds> oddsList) {
        for (Odds odds : oddsList) {
            BestOdds bestOdds = new BestOdds();

            // Initialize best outcomes and bookmakers for each market
            Outcome bestH2HHomeOutcome = null;
            Bookmaker bestH2HHomeBookmaker = null;

            Outcome bestH2HAwayOutcome = null;
            Bookmaker bestH2HAwayBookmaker = null;

            Outcome bestSpreadHomeOutcome = null;
            Bookmaker bestSpreadHomeBookmaker = null;

            Outcome bestSpreadAwayOutcome = null;
            Bookmaker bestSpreadAwayBookmaker = null;

            Outcome bestOverOutcome = null;
            Bookmaker bestOverBookmaker = null;

            Outcome bestUnderOutcome = null;
            Bookmaker bestUnderBookmaker = null;

            // For each bookmaker
            for (Bookmaker bookmaker : odds.getBookmakers()) {
                // For each market
                for (Market market : bookmaker.getMarkets()) {
                    String marketKey = market.getKey();

                    // H2H Market
                    if ("h2h".equals(marketKey)) {
                        for (Outcome outcome : market.getOutcomes()) {
                            // Home Team
                            if (odds.getHomeTeam().equals(outcome.getName())) {
                                if (bestH2HHomeOutcome == null || outcome.getPrice() > bestH2HHomeOutcome.getPrice()) {
                                    bestH2HHomeOutcome = outcome;
                                    bestH2HHomeBookmaker = bookmaker;
                                }
                            }
                            // Away Team
                            else if (odds.getAwayTeam().equals(outcome.getName())) {
                                if (bestH2HAwayOutcome == null || outcome.getPrice() > bestH2HAwayOutcome.getPrice()) {
                                    bestH2HAwayOutcome = outcome;
                                    bestH2HAwayBookmaker = bookmaker;
                                }
                            }
                        }
                    }

                    // Spreads Market
                    else if ("spreads".equals(marketKey)) {
                        for (Outcome outcome : market.getOutcomes()) {
                            // Home Team
                            if (odds.getHomeTeam().equals(outcome.getName())) {
                                if (isBetterSpread(outcome, bestSpreadHomeOutcome, true)) {
                                    bestSpreadHomeOutcome = outcome;
                                    bestSpreadHomeBookmaker = bookmaker;
                                }
                            }
                            // Away Team
                            else if (odds.getAwayTeam().equals(outcome.getName())) {
                                if (isBetterSpread(outcome, bestSpreadAwayOutcome, false)) {
                                    bestSpreadAwayOutcome = outcome;
                                    bestSpreadAwayBookmaker = bookmaker;
                                }
                            }
                        }
                    }

                    // Totals Market
                    else if ("totals".equals(marketKey)) {
                        for (Outcome outcome : market.getOutcomes()) {
                            // Over
                            if ("Over".equalsIgnoreCase(outcome.getName())) {
                                if (isBetterTotalOver(outcome, bestOverOutcome)) {
                                    bestOverOutcome = outcome;
                                    bestOverBookmaker = bookmaker;
                                }
                            }
                            // Under
                            else if ("Under".equalsIgnoreCase(outcome.getName())) {
                                if (isBetterTotalUnder(outcome, bestUnderOutcome)) {
                                    bestUnderOutcome = outcome;
                                    bestUnderBookmaker = bookmaker;
                                }
                            }
                        }
                    }
                }
            }

            // Set the best outcomes and bookmakers in the BestOdds object
            bestOdds.setBestH2HHomeOutcome(bestH2HHomeOutcome);
            bestOdds.setBestH2HHomeBookmaker(bestH2HHomeBookmaker);

            bestOdds.setBestH2HAwayOutcome(bestH2HAwayOutcome);
            bestOdds.setBestH2HAwayBookmaker(bestH2HAwayBookmaker);

            bestOdds.setBestSpreadHomeOutcome(bestSpreadHomeOutcome);
            bestOdds.setBestSpreadHomeBookmaker(bestSpreadHomeBookmaker);

            bestOdds.setBestSpreadAwayOutcome(bestSpreadAwayOutcome);
            bestOdds.setBestSpreadAwayBookmaker(bestSpreadAwayBookmaker);

            bestOdds.setBestOverOutcome(bestOverOutcome);
            bestOdds.setBestOverBookmaker(bestOverBookmaker);

            bestOdds.setBestUnderOutcome(bestUnderOutcome);
            bestOdds.setBestUnderBookmaker(bestUnderBookmaker);

            // Set the BestOdds object in the Odds object
            odds.setBestOdds(bestOdds);
        }
    }

    // Helper method to determine if a new spread is better than the current best
    private boolean isBetterSpread(Outcome newOutcome, Outcome currentBest, boolean isHomeTeam) {
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
    private boolean isBetterTotalOver(Outcome newOutcome, Outcome currentBest) {
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
    private boolean isBetterTotalUnder(Outcome newOutcome, Outcome currentBest) {
        if (newOutcome == null) return false;
        if (currentBest == null) return true;

        Double newPoint = newOutcome.getPoint();
        Double currentPoint = currentBest.getPoint();

        // Higher total is better for Under bets
        if (newPoint > currentPoint) return true;
        if (newPoint.equals(currentPoint) && newOutcome.getPrice() > currentBest.getPrice()) return true;

        return false;
    }

    // Helper method to map sport keys to user-friendly names
    private String getSportName(String sportKey) {
        Map<String, String> sportNames = new HashMap<>();
        sportNames.put("americanfootball_nfl", "NFL");
        sportNames.put("basketball_nba", "NBA");
        sportNames.put("americanfootball_ncaaf", "NCAAF");
        sportNames.put("baseball_mlb", "MLB");
        sportNames.put("basketball_wnba", "WNBA");
        sportNames.put("icehockey_nhl", "NHL");
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
        
        Map<String, String> mlb = new HashMap<>();
        mlb.put("key", "baseball_mlb");
        mlb.put("name", "MLB");
        availableSports.add(mlb);

        Map<String, String> wnba = new HashMap<>();
        wnba.put("key", "basketball_wnba");
        wnba.put("name", "WNBA");
        availableSports.add(wnba);
       

        Map<String, String> nhl = new HashMap<>();
        nhl.put("key", "icehockey_nhl");
        nhl.put("name", "NHL");
        availableSports.add(nhl);
        // Add more sports as needed

        return availableSports;
    }

    // Helper method to get unique bookmakers offering the specified market type
    private Set<Bookmaker> getUniqueBookmakers(List<Odds> oddsList, String marketType) {
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
    private Map<String, Map<String, Bookmaker>> getMatchupBookmakerMap(List<Odds> oddsList, String marketType) {
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

}

