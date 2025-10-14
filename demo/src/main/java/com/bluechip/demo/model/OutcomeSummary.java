package com.bluechip.demo.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Enriched representation of the best available odds for a single outcome,
 * including the fair price consensus and the resulting edge (if available).
 */
@Getter
@Setter
public class OutcomeSummary {
    private String marketType;        // h2h, spreads, totals, etc.
    private String outcomeKey;        // HOME, AWAY, OVER, UNDER
    private String outcomeName;       // Team name or Over/Under label
    private Double point;             // Spread/total number when applicable

    private String bookmakerTitle;    // Sportsbook offering the best price
    private Integer bestAmerican;     // American odds for the best book
    private Double bestDecimal;       // Decimal odds for the best book

    private Double fairProbability;   // Consensus probability (0-1)
    private Double fairDecimal;       // Corresponding fair decimal odds
    private Integer fairAmerican;     // Fair American price derived from fairDecimal

    private Double edge;              // Expected value (p * decimal - 1)
}
