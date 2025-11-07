package com.bluechip.demo.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.bluechip.demo.model.BestOdds;
import com.bluechip.demo.model.Bookmaker;
import com.bluechip.demo.model.Market;
import com.bluechip.demo.model.Odds;
import com.bluechip.demo.model.Outcome;
import com.bluechip.demo.util.Utilities;

@Service
public class BestOddsService {
    
    // New method to compute best odds for each Odds object
    public void computeBestOdds(List<Odds> oddsList) {
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
                                if (Utilities.isBetterSpread(outcome, bestSpreadHomeOutcome, true)) {
                                    bestSpreadHomeOutcome = outcome;
                                    bestSpreadHomeBookmaker = bookmaker;
                                }
                            }
                            // Away Team
                            else if (odds.getAwayTeam().equals(outcome.getName())) {
                                if (Utilities.isBetterSpread(outcome, bestSpreadAwayOutcome, false)) {
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
                                if (Utilities.isBetterTotalOver(outcome, bestOverOutcome)) {
                                    bestOverOutcome = outcome;
                                    bestOverBookmaker = bookmaker;
                                }
                            }
                            // Under
                            else if ("Under".equalsIgnoreCase(outcome.getName())) {
                                if (Utilities.isBetterTotalUnder(outcome, bestUnderOutcome)) {
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
}
