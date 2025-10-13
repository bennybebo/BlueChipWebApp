package com.bluechip.demo.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BestOdds {
    // Best H2H Odds
    private Bookmaker bestH2HHomeBookmaker;
    private Outcome bestH2HHomeOutcome;

    private Bookmaker bestH2HAwayBookmaker;
    private Outcome bestH2HAwayOutcome;

    // Best Spreads
    private Bookmaker bestSpreadHomeBookmaker;
    private Outcome bestSpreadHomeOutcome;

    private Bookmaker bestSpreadAwayBookmaker;
    private Outcome bestSpreadAwayOutcome;

    // Best Totals (Over and Under)
    private Bookmaker bestOverBookmaker;
    private Outcome bestOverOutcome;

    private Bookmaker bestUnderBookmaker;
    private Outcome bestUnderOutcome;

}
