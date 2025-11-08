package com.bluechip.demo.model;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FairPrices {
    private double homeProb;      // 0..1
    private double awayProb;      // 0..1
    private double homeDecimal;   // 1/p
    private double awayDecimal;   // 1/p
    private int homeAmerican;     // from decimal
    private int awayAmerican;     // from decimal
}
