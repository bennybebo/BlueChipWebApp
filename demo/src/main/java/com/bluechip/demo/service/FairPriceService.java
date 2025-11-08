package com.bluechip.demo.service;

import com.bluechip.demo.model.Bookmaker;
import com.bluechip.demo.model.FairPrices;
import com.bluechip.demo.model.Market;
import com.bluechip.demo.model.Odds;
import com.bluechip.demo.model.Outcome;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Fair odds from market consensus without historical calibration:
 * 1) For each bookmaker that quotes both sides on the same market:
 *    - Convert to decimal, compute raw implied probs (1/decimal)
 *    - De-vig per book by normalizing q's to sum to 1
 * 2) Convert the per-book probabilities to logits
 * 3) Robustly trim outliers via median/MAD on logits
 * 4) Equal-weight average remaining logits, back-transform to probabilities
 * 5) Convert to fair odds (decimal & American)
 */
@Service
public class FairPriceService {

    // ---------- Tunables ----------
    private static final double DEFAULT_ZMAX = 3.0;  // trimming threshold on robust z-scores
    private static final double EPS = 1e-6;          // clamp for probabilities
    // ------------------------------

    /**
     * Computes fair prices for the given marketType across a list of Odds.
     * Currently implemented for "h2h". (Spreads/Totals can be added similarly.)
     */
    public void computeFairPrice(List<Odds> oddsList, String marketType) {
        if (oddsList == null || oddsList.isEmpty()) return;

        if (!"h2h".equalsIgnoreCase(marketType)) {
            // Future: implement spreads/totals with similar per-book pairing & de-vig.
            return;
        }

        for (Odds odds : oddsList) {
            if (odds == null || odds.getBookmakers() == null) continue;

            // Collect per-book probabilities for HOME (away is 1 - home)
            List<Double> homeProbs = new ArrayList<>();

            for (Bookmaker bookmaker : odds.getBookmakers()) {
                if (bookmaker == null || bookmaker.getMarkets() == null) continue;

                // find the H2H market for this bookmaker
                Market h2h = null;
                for (Market m : bookmaker.getMarkets()) {
                    if (m != null && "h2h".equalsIgnoreCase(m.getKey())) {
                        h2h = m; break;
                    }
                }
                if (h2h == null || h2h.getOutcomes() == null) continue;

                Integer homeAmerican = null;
                Integer awayAmerican = null;

                for (Outcome outcome : h2h.getOutcomes()) {
                    if (outcome == null) continue;
                    // Outcomes are named as team names in your BestOdds code
                    if (odds.getHomeTeam() != null && odds.getHomeTeam().equals(outcome.getName())) {
                        homeAmerican = outcome.getPrice();
                    } else if (odds.getAwayTeam() != null && odds.getAwayTeam().equals(outcome.getName())) {
                        awayAmerican = outcome.getPrice();
                    }
                }

                if (homeAmerican == null || awayAmerican == null) continue;

                double dH = americanToDecimal(homeAmerican);
                double dA = americanToDecimal(awayAmerican);
                if (!Double.isFinite(dH) || !Double.isFinite(dA)) continue;

                double qH = 1.0 / dH;
                double qA = 1.0 / dA;
                double sum = qH + qA;
                if (!(sum > 0.0) || !Double.isFinite(sum)) continue;

                double pH = qH / sum;                 // per-book de-vig
                pH = clamp(pH, EPS, 1.0 - EPS);       // stabilize for logit
                homeProbs.add(pH);
            }

            odds.setFairPrices(fairFromHomeProbsEqualWeightedTrimmed(homeProbs));
        }
    }

    /**
     * Core aggregator: equal-weight, robustly trimmed average in logit space.
     * Input: list of per-book HOME probabilities after de-vig.
     */
    private FairPrices fairFromHomeProbsEqualWeightedTrimmed(List<Double> pHomeList) {
        if (pHomeList == null || pHomeList.isEmpty()) return null;

        // Convert to logits
        double[] logits = new double[pHomeList.size()];
        for (int i = 0; i < pHomeList.size(); i++) {
            double p = clamp(pHomeList.get(i), EPS, 1.0 - EPS);
            logits[i] = logit(p);
        }

        // Robust keep mask via median/MAD
        boolean[] keep = robustKeepMask(logits, DEFAULT_ZMAX);
        int kept = 0; double sumL = 0.0;
        for (int i = 0; i < logits.length; i++) {
            if (keep[i]) { kept++; sumL += logits[i]; }
        }

        if (kept == 0) {
            // Fallback to median logit if everything got trimmed
            double med = median(logits);
            kept = 1; sumL = med;
        }

        double lBar = sumL / kept;
        double pH = invLogit(lBar);
        double pA = 1.0 - pH;

        double dHfair = 1.0 / pH;
        double dAfair = 1.0 / pA;

        return new FairPrices(
                pH, pA,
                r2(dHfair), r2(dAfair),
                decimalToAmerican(dHfair),
                decimalToAmerican(dAfair)
        );
    }

    // -------------------- Numeric helpers --------------------

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double logit(double p) { return Math.log(p / (1.0 - p)); }

    private static double invLogit(double l) {
        // numerically stable logistic
        if (l >= 0) {
            double ez = Math.exp(-l);
            return 1.0 / (1.0 + ez);
        } else {
            double ez = Math.exp(l);
            return ez / (1.0 + ez);
        }
    }

    private static boolean[] robustKeepMask(double[] x, double zMax) {
        int n = x.length;
        boolean[] keep = new boolean[n];
        Arrays.fill(keep, true);
        if (n <= 2) return keep; // not enough to trim

        double med = median(x);
        double[] absDev = new double[n];
        for (int i = 0; i < n; i++) absDev[i] = Math.abs(x[i] - med);
        double mad = median(absDev);

        if (mad == 0.0) return keep; // identical; keep all

        // "Robust" z-score: 0.6745*(x - median)/MAD
        for (int i = 0; i < n; i++) {
            double z = 0.6745 * (x[i] - med) / mad;
            if (Math.abs(z) > zMax) keep[i] = false;
        }
        return keep;
    }

    private static double median(double[] arr) {
        double[] a = arr.clone();
        Arrays.sort(a);
        int n = a.length;
        return (n % 2 == 1) ? a[n / 2] : 0.5 * (a[n / 2 - 1] + a[n / 2]);
    }
    
    public double americanToDecimal(Integer american) {
        if (american == null) return Double.NaN;
        return (american > 0)
                ? 1.0 + (american / 100.0)
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