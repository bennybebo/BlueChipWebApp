package com.bluechip.demo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bluechip.demo.model.Outcome;

public class Utilities {
    public static final List<Map<String, String>> AVAILABLE_SPORTS = List.of(
        Map.of("key", "americanfootball_nfl", "name", "NFL"),
        Map.of("key", "basketball_nba", "name", "NBA"),
        Map.of("key", "americanfootball_ncaaf", "name", "NCAAF"),
        Map.of("key", "baseball_mlb", "name", "MLB"),
        Map.of("key", "basketball_wnba", "name", "WNBA"),
        Map.of("key", "icehockey_nhl", "name", "NHL")
    );

    private static final Map<String, String> SPORT_NAMES = new HashMap<>() {{
        put("americanfootball_nfl", "NFL");
        put("basketball_nba", "NBA");
        put("americanfootball_ncaaf", "NCAAF");
        put("baseball_mlb", "MLB");
        put("basketball_wnba", "WNBA");
        put("icehockey_nhl", "NHL");
    }};

    public static String getSportName(String sportKey) {
        return SPORT_NAMES.getOrDefault(sportKey, sportKey);
    }

    public static boolean isBetterSpread(Outcome newOutcome, Outcome currentBest, boolean isHomeTeam) {
        if (newOutcome == null) return false;
        if (currentBest == null) return true;
        Double newPoint = newOutcome.getPoint();
        Double currentPoint = currentBest.getPoint();
        if (isHomeTeam) {
            if (newPoint < 0 && currentPoint < 0) {
                if (newPoint > currentPoint) return true;
                if (newPoint.equals(currentPoint) && newOutcome.getPrice() > currentBest.getPrice()) return true;
            } else if (newPoint >= 0 && currentPoint < 0) {
                return true;
            } else if (newPoint >= 0 && currentPoint >= 0) {
                if (newPoint > currentPoint) return true;
                if (newPoint.equals(currentPoint) && newOutcome.getPrice() > currentBest.getPrice()) return true;
            }
        } else {
            if (newPoint > currentPoint) return true;
            if (newPoint.equals(currentPoint) && newOutcome.getPrice() > currentBest.getPrice()) return true;
        }
        return false;
    }

    public static boolean isBetterTotalOver(Outcome newOutcome, Outcome currentBest) {
        if (newOutcome == null) return false;
        if (currentBest == null) return true;
        Double newPoint = newOutcome.getPoint();
        Double currentPoint = currentBest.getPoint();
        if (newPoint < currentPoint) return true;
        if (newPoint.equals(currentPoint) && newOutcome.getPrice() > currentBest.getPrice()) return true;
        return false;
    }

    public static boolean isBetterTotalUnder(Outcome newOutcome, Outcome currentBest) {
        if (newOutcome == null) return false;
        if (currentBest == null) return true;
        Double newPoint = newOutcome.getPoint();
        Double currentPoint = currentBest.getPoint();
        if (newPoint > currentPoint) return true;
        if (newPoint.equals(currentPoint) && newOutcome.getPrice() > currentBest.getPrice()) return true;
        return false;
    }

    public static BigDecimal americanToDecimal(Integer american) {
        if (american == null) return null;
        int a = american;
        if (a > 0) {
            return BigDecimal.ONE.add(BigDecimal.valueOf(a).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
                    .setScale(3, RoundingMode.HALF_UP);
        } else {
            return BigDecimal.ONE.add(BigDecimal.valueOf(100).divide(BigDecimal.valueOf(-a), 6, RoundingMode.HALF_UP))
                    .setScale(3, RoundingMode.HALF_UP);
        }
    }
}
