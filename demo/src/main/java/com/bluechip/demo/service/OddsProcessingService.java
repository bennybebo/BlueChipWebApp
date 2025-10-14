package com.bluechip.demo.service;

import com.bluechip.demo.dto.FairPriceDto;
import com.bluechip.demo.model.*;
import com.bluechip.demo.repositories.OddsOutcomeSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class OddsProcessingService {

    private final OddsService oddsService;
    private final BestOddsService bestOddsService;
    private final FairPriceService fairPriceService;
    private final OddsOutcomeSnapshotRepository snapshotRepository;

    public OddsProcessingService(
            OddsService oddsService,
            BestOddsService bestOddsService,
            FairPriceService fairPriceService,
            OddsOutcomeSnapshotRepository snapshotRepository) {
        this.oddsService = oddsService;
        this.bestOddsService = bestOddsService;
        this.fairPriceService = fairPriceService;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public ProcessedOddsResult processAndStore(String sportKey, String marketType) throws IOException {
        List<Odds> oddsList = oddsService.getOddsData(sportKey, marketType);
        oddsList.sort(Comparator.comparing(Odds::getCommenceTime, Comparator.nullsLast(String::compareTo)));

        bestOddsService.computeBestOdds(oddsList);
        Map<String, FairPriceDto> fairMap = fairPriceService.computeFairPrices(marketType, oddsList);

        Instant refreshedAt = Instant.now();
        List<OddsOutcomeSnapshot> snapshots = new ArrayList<>();

        for (Odds odds : oddsList) {
            String matchupKey = buildMatchupKey(odds);
            FairPriceDto fair = fairMap.get(matchupKey);

            List<OutcomeSummary> summaries = buildOutcomeSummaries(odds, marketType, fair);
            odds.setOutcomeSummaries(summaries);

            for (OutcomeSummary summary : summaries) {
                snapshots.add(toSnapshot(odds, summary, sportKey, marketType, refreshedAt));
            }
        }

        snapshotRepository.deleteBySportKeyAndMarketType(sportKey, marketType);
        if (!snapshots.isEmpty()) {
            snapshotRepository.saveAll(snapshots);
        }

        return new ProcessedOddsResult(oddsList, fairMap);
    }

    private List<OutcomeSummary> buildOutcomeSummaries(Odds odds, String marketType, FairPriceDto fair) {
        List<OutcomeSummary> summaries = new ArrayList<>();
        BestOdds bestOdds = odds.getBestOdds();
        if (bestOdds == null) {
            return summaries;
        }

        switch (marketType) {
            case "h2h" -> {
                addSummary(summaries, marketType, "HOME", odds.getHomeTeam(), bestOdds.getBestH2HHomeOutcome(),
                        bestOdds.getBestH2HHomeBookmaker(), fair != null ? fair.getHomeProb() : null,
                        fair != null ? fair.getHomeDecimal() : null, fair != null ? fair.getHomeAmerican() : null);
                addSummary(summaries, marketType, "AWAY", odds.getAwayTeam(), bestOdds.getBestH2HAwayOutcome(),
                        bestOdds.getBestH2HAwayBookmaker(), fair != null ? fair.getAwayProb() : null,
                        fair != null ? fair.getAwayDecimal() : null, fair != null ? fair.getAwayAmerican() : null);
            }
            case "spreads" -> {
                addSummary(summaries, marketType, "HOME", odds.getHomeTeam(), bestOdds.getBestSpreadHomeOutcome(),
                        bestOdds.getBestSpreadHomeBookmaker(), null, null, null);
                addSummary(summaries, marketType, "AWAY", odds.getAwayTeam(), bestOdds.getBestSpreadAwayOutcome(),
                        bestOdds.getBestSpreadAwayBookmaker(), null, null, null);
            }
            case "totals" -> {
                addSummary(summaries, marketType, "OVER", "Over", bestOdds.getBestOverOutcome(),
                        bestOdds.getBestOverBookmaker(), null, null, null);
                addSummary(summaries, marketType, "UNDER", "Under", bestOdds.getBestUnderOutcome(),
                        bestOdds.getBestUnderBookmaker(), null, null, null);
            }
            default -> {
                // Unsupported market, nothing to add.
            }
        }

        return summaries;
    }

    private void addSummary(List<OutcomeSummary> summaries,
                            String marketType,
                            String outcomeKey,
                            String outcomeName,
                            Outcome outcome,
                            Bookmaker bookmaker,
                            Double fairProb,
                            Double fairDecimal,
                            Integer fairAmerican) {
        if (outcome == null || bookmaker == null) {
            return;
        }

        double decimal = fairPriceService.americanToDecimal(outcome.getPrice());
        if (!Double.isFinite(decimal)) {
            return;
        }

        OutcomeSummary summary = new OutcomeSummary();
        summary.setMarketType(marketType);
        summary.setOutcomeKey(outcomeKey);
        summary.setOutcomeName(outcomeName);
        Double point = null;
        if ("spreads".equals(marketType) || "totals".equals(marketType)) {
            point = Double.isNaN(outcome.getPoint()) ? null : outcome.getPoint();
        }
        summary.setPoint(point);
        summary.setBookmakerTitle(bookmaker.getTitle());
        summary.setBestAmerican(outcome.getPrice());
        summary.setBestDecimal(decimal);
        summary.setFairProbability(fairProb);
        summary.setFairDecimal(fairDecimal);
        summary.setFairAmerican(fairAmerican);
        summary.setEdge(fairProb != null ? fairProb * decimal - 1.0 : null);
        summaries.add(summary);
    }

    private OddsOutcomeSnapshot toSnapshot(Odds odds,
                                           OutcomeSummary summary,
                                           String sportKey,
                                           String marketType,
                                           Instant refreshedAt) {
        OddsOutcomeSnapshot snapshot = new OddsOutcomeSnapshot();
        snapshot.setSportKey(sportKey);
        snapshot.setSportTitle(odds.getSportTitle());
        snapshot.setMarketType(marketType);
        snapshot.setEventId(odds.getId());

        ZonedDateTime commence = odds.getCommenceTimeAsZonedDateTime();
        snapshot.setEventCommence(commence != null ? commence.toInstant() : null);

        snapshot.setHomeTeam(odds.getHomeTeam());
        snapshot.setAwayTeam(odds.getAwayTeam());
        snapshot.setOutcomeKey(summary.getOutcomeKey());
        snapshot.setOutcomeName(summary.getOutcomeName());
        snapshot.setPoint(summary.getPoint());
        snapshot.setBookmakerTitle(summary.getBookmakerTitle());
        snapshot.setBestAmerican(summary.getBestAmerican());
        snapshot.setBestDecimal(summary.getBestDecimal());
        snapshot.setFairProbability(summary.getFairProbability());
        snapshot.setFairDecimal(summary.getFairDecimal());
        snapshot.setFairAmerican(summary.getFairAmerican());
        snapshot.setEdge(summary.getEdge());
        snapshot.setRefreshedAt(refreshedAt);
        return snapshot;
    }

    private String buildMatchupKey(Odds odds) {
        String home = Optional.ofNullable(odds.getHomeTeam()).orElse("Home");
        String away = Optional.ofNullable(odds.getAwayTeam()).orElse("Away");
        return home + " vs " + away;
    }

    public record ProcessedOddsResult(List<Odds> oddsList, Map<String, FairPriceDto> fairPrices) {}
}
