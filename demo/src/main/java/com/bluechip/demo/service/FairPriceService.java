package com.bluechip.demo.service;

import com.bluechip.demo.dto.FairPriceDto;
import com.bluechip.demo.model.BestOdds;
import com.bluechip.demo.model.Odds;
import com.bluechip.demo.model.Outcome;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class FairPriceService {

    // TODO: Calculate Fair Prices for other market types (spread, totals) and use average of books instead of Best Odds
    public Map<String, FairPriceDto> computeFairPrices (String marketType, List<Odds> oddsList) {
        Map<String, FairPriceDto> fairMap = new HashMap<>();
            if ("h2h".equalsIgnoreCase(marketType)) {
                for (Odds odds : oddsList) {
                    BestOdds best = odds.getBestOdds();
                    Outcome homeOutcome = (best != null) ? best.getBestH2HHomeOutcome() : null;
                    Outcome awayOutcome = (best != null) ? best.getBestH2HAwayOutcome() : null;

                    Integer homePrice = (homeOutcome != null) ? homeOutcome.getPrice() : null;
                    Integer awayPrice = (awayOutcome != null) ? awayOutcome.getPrice() : null;

                    FairPriceDto fair = fairFromTwoMoneylines(homePrice, awayPrice);
                    if (fair != null) {
                        String key = odds.getHomeTeam() + " vs " + odds.getAwayTeam();
                        fairMap.put(key, fair);
                    }
                }
            }
        return fairMap;
    }

    public FairPriceDto fairFromTwoMoneylines(Integer homeAmerican, Integer awayAmerican) {
        if (homeAmerican == null || awayAmerican == null) return null;

        double dH = americanToDecimal(homeAmerican);
        double dA = americanToDecimal(awayAmerican);
        if (!Double.isFinite(dH) || !Double.isFinite(dA)) return null;

        double pHprime = 1.0 / dH;
        double pAprime = 1.0 / dA;

        double sum = pHprime + pAprime;
        if (sum <= 0) return null;

        double pH = pHprime / sum;
        double pA = pAprime / sum;

        double dHfair = 1.0 / pH;
        double dAfair = 1.0 / pA;

        return new FairPriceDto(
                pH, pA,
                r2(dHfair), r2(dAfair),
                decimalToAmerican(dHfair),
                decimalToAmerican(dAfair)
        );
    }

    /* ---- pure helpers ---- */
    public double americanToDecimal(Integer american) {
        if (american == null) return Double.NaN;
        return american > 0 ? 1.0 + (american / 100.0)
                            : 1.0 + (100.0 / Math.abs(american));
    }

    public int decimalToAmerican(double dec) {
        if (dec <= 1.0) return 0;
        return (dec >= 2.0)
                ? (int) Math.round((dec - 1.0) * 100.0)
                : (int) Math.round(-100.0 / (dec - 1.0));
    }
    public double r2(double v) { 
        return Math.round(v * 100.0) / 100.0; 
    }
}