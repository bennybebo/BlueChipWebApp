package com.bluechip.demo.service;

import com.bluechip.demo.dto.FairPriceDto;
import com.bluechip.demo.model.*;
import com.bluechip.demo.repositories.OddsOutcomeSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
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

    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofMinutes(1);

    @Transactional
    public ProcessedOddsResult processAndStore(String sportKey, String marketType) throws IOException {
        boolean refreshRequired = isRefreshRequired(sportKey, marketType);
        List<Odds> oddsList = oddsService.getOddsData(sportKey, marketType, refreshRequired);
        oddsList.sort(Comparator.comparing(Odds::getCommenceTime, Comparator.nullsLast(String::compareTo)));

        bestOddsService.computeBestOdds(oddsList);
        Map<String, FairPriceDto> fairMap = fairPriceService.computeFairPrices(marketType, oddsList);

        Instant refreshedAt = refreshRequired
                ? Instant.now()
                : snapshotRepository.findMostRecentRefresh(sportKey, marketType).orElse(Instant.now());
        Map<String, OddsOutcomeSnapshot> snapshotMap = new LinkedHashMap<>();

        for (Odds odds : oddsList) {
            String matchupKey = buildMatchupKey(odds);
            FairPriceDto fair = fairMap.get(matchupKey);

            List<OutcomeSummary> summaries = buildOutcomeSummaries(odds, marketType, fair);
            odds.setOutcomeSummaries(summaries);

            if (refreshRequired) {
                for (OutcomeSummary summary : summaries) {
                    OddsOutcomeSnapshot snapshot = toSnapshot(odds, summary, sportKey, marketType, refreshedAt);
                    String snapshotKey = buildSnapshotKey(snapshot);
                    OddsOutcomeSnapshot existing = snapshotMap.get(snapshotKey);
                    if (shouldReplaceSnapshot(snapshot, existing)) {
                        snapshotMap.put(snapshotKey, snapshot);
                    }
                }
            }
        }

        if (refreshRequired) {
            snapshotRepository.deleteBySportKeyAndMarketType(sportKey, marketType);
            if (!snapshotMap.isEmpty()) {
                snapshotRepository.saveAll(snapshotMap.values());
            }
        }

        return new ProcessedOddsResult(oddsList, fairMap, refreshedAt, refreshRequired);
    }

    private boolean isRefreshRequired(String sportKey, String marketType) {
        Optional<Instant> lastRefresh = snapshotRepository.findMostRecentRefresh(sportKey, marketType);
        if (lastRefresh.isEmpty()) {
            return true;
        }
        Instant last = lastRefresh.get();
        return last.plus(MIN_REFRESH_INTERVAL).isBefore(Instant.now());
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
        String eventId = Optional.ofNullable(odds.getId()).orElse(buildMatchupKey(odds));
        if (eventId != null && eventId.length() > 128) {
            eventId = eventId.substring(0, 128);
        }
        snapshot.setEventId(eventId);

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

    private String buildSnapshotKey(OddsOutcomeSnapshot snapshot) {
        return String.join("::",
                Optional.ofNullable(snapshot.getSportKey()).orElse(""),
                Optional.ofNullable(snapshot.getMarketType()).orElse(""),
                Optional.ofNullable(snapshot.getEventId()).orElse(""),
                Optional.ofNullable(snapshot.getOutcomeKey()).orElse(""));
    }

    private boolean shouldReplaceSnapshot(OddsOutcomeSnapshot candidate, OddsOutcomeSnapshot existing) {
        if (candidate == null) {
            return false;
        }
        if (existing == null) {
            return true;
        }

        Instant candidateRefresh = candidate.getRefreshedAt();
        Instant existingRefresh = existing.getRefreshedAt();
        if (candidateRefresh != null && existingRefresh != null && candidateRefresh.isAfter(existingRefresh)) {
            return true;
        }
        if (candidateRefresh != null && existingRefresh == null) {
            return true;
        }

        Double candidateEdge = candidate.getEdge();
        Double existingEdge = existing.getEdge();
        if (candidateEdge != null && (existingEdge == null || Double.compare(candidateEdge, existingEdge) > 0)) {
            return true;
        }

        Double candidateDecimal = candidate.getBestDecimal();
        Double existingDecimal = existing.getBestDecimal();
        return candidateDecimal != null && existingDecimal == null;
    }

    private String buildMatchupKey(Odds odds) {
        String home = Optional.ofNullable(odds.getHomeTeam()).orElse("Home");
        String away = Optional.ofNullable(odds.getAwayTeam()).orElse("Away");
        return home + " vs " + away;
    }

    public record ProcessedOddsResult(List<Odds> oddsList,
                                      Map<String, FairPriceDto> fairPrices,
                                      Instant refreshedAt,
                                      boolean refreshed) {}
}
