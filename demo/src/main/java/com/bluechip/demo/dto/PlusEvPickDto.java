package com.bluechip.demo.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PlusEvPickDto {
    private String formattedCommenceTime;
    private String category;                 // e.g., NFL, NCAAF, NBA
    private String sportName;                // optional display
    private String homeTeam;
    private String awayTeam;
    private String market;                   // h2h, spreads, totals
    private String outcome;                  // HOME/AWAY/OVER/UNDER

    private double fairProb;                 // 0–1
    private double fairDecimal;              // 1/p

    private String bookmakerTitle;           // e.g., DraftKings
    private int offeredAmerican;             // e.g., +125
    private double offeredDecimal;           // from American

    private double edgePct;                  // p*d - 1, in %
    private double recommendedStakeDollars;  // bankroll * fractional Kelly
}
