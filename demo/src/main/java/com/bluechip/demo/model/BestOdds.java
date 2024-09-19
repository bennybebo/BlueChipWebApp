package com.bluechip.demo.model;

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

    // Getters and Setters
    public Bookmaker getBestH2HHomeBookmaker() {
        return bestH2HHomeBookmaker;
    }

    public void setBestH2HHomeBookmaker(Bookmaker bestH2HHomeBookmaker) {
        this.bestH2HHomeBookmaker = bestH2HHomeBookmaker;
    }

    public Outcome getBestH2HHomeOutcome() {
        return bestH2HHomeOutcome;
    }

    public void setBestH2HHomeOutcome(Outcome bestH2HHomeOutcome) {
        this.bestH2HHomeOutcome = bestH2HHomeOutcome;
    }

    // Best H2H Away Odds
    public Bookmaker getBestH2HAwayBookmaker() {
        return bestH2HAwayBookmaker;
    }

    public void setBestH2HAwayBookmaker(Bookmaker bestH2HAwayBookmaker) {
        this.bestH2HAwayBookmaker = bestH2HAwayBookmaker;
    }

    public Outcome getBestH2HAwayOutcome() {
        return bestH2HAwayOutcome;
    }

    public void setBestH2HAwayOutcome(Outcome bestH2HAwayOutcome) {
        this.bestH2HAwayOutcome = bestH2HAwayOutcome;
    }

    // Best Spread Home
    public Bookmaker getBestSpreadHomeBookmaker() {
        return bestSpreadHomeBookmaker;
    }

    public void setBestSpreadHomeBookmaker(Bookmaker bestSpreadHomeBookmaker) {
        this.bestSpreadHomeBookmaker = bestSpreadHomeBookmaker;
    }

    public Outcome getBestSpreadHomeOutcome() {
        return bestSpreadHomeOutcome;
    }

    public void setBestSpreadHomeOutcome(Outcome bestSpreadHomeOutcome) {
        this.bestSpreadHomeOutcome = bestSpreadHomeOutcome;
    }

    // Best Spread Away
    public Bookmaker getBestSpreadAwayBookmaker() {
        return bestSpreadAwayBookmaker;
    }

    public void setBestSpreadAwayBookmaker(Bookmaker bestSpreadAwayBookmaker) {
        this.bestSpreadAwayBookmaker = bestSpreadAwayBookmaker;
    }

    public Outcome getBestSpreadAwayOutcome() {
        return bestSpreadAwayOutcome;
    }

    public void setBestSpreadAwayOutcome(Outcome bestSpreadAwayOutcome) {
        this.bestSpreadAwayOutcome = bestSpreadAwayOutcome;
    }

    // Best Over Odds
    public Bookmaker getBestOverBookmaker() {
        return bestOverBookmaker;
    }

    public void setBestOverBookmaker(Bookmaker bestOverBookmaker) {
        this.bestOverBookmaker = bestOverBookmaker;
    }

    public Outcome getBestOverOutcome() {
        return bestOverOutcome;
    }

    public void setBestOverOutcome(Outcome bestOverOutcome) {
        this.bestOverOutcome = bestOverOutcome;
    }

    // Best Under Odds
    public Bookmaker getBestUnderBookmaker() {
        return bestUnderBookmaker;
    }

    public void setBestUnderBookmaker(Bookmaker bestUnderBookmaker) {
        this.bestUnderBookmaker = bestUnderBookmaker;
    }

    public Outcome getBestUnderOutcome() {
        return bestUnderOutcome;
    }

    public void setBestUnderOutcome(Outcome bestUnderOutcome) {
        this.bestUnderOutcome = bestUnderOutcome;
    }
}
