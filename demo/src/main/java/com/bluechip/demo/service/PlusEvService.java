package com.bluechip.demo.service;

import com.bluechip.demo.dto.PlusEvPickDto;
import com.bluechip.demo.model.Bookmaker;
import com.bluechip.demo.model.OddsOutcomeSnapshot;
import com.bluechip.demo.repositories.OddsOutcomeSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PlusEvService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("M/d - h:mm a")
            .withZone(DISPLAY_ZONE);

    private final OddsOutcomeSnapshotRepository snapshotRepository;

    public PlusEvService(OddsOutcomeSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    public List<PlusEvPickDto> loadPlusEvPicks(double bankrollDollars, double kellyFraction) {
        List<OddsOutcomeSnapshot> snapshots = snapshotRepository
                .findByEdgeNotNullAndEdgeGreaterThanOrderByEdgeDesc(0.0);

        List<PlusEvPickDto> picks = new ArrayList<>();
        for (OddsOutcomeSnapshot snapshot : snapshots) {
            PlusEvPickDto dto = toDto(snapshot, bankrollDollars, kellyFraction);
            if (dto != null) {
                picks.add(dto);
            }
        }
        return picks;
    }

    public List<Bookmaker> deriveBookmakers(List<PlusEvPickDto> picks) {
        Map<String, Bookmaker> map = new LinkedHashMap<>();
        for (PlusEvPickDto pick : picks) {
            String title = pick.getBookmakerTitle();
            if (title == null || title.isBlank()) {
                continue;
            }
            map.computeIfAbsent(title, t -> {
                Bookmaker bookmaker = new Bookmaker();
                bookmaker.setTitle(t);
                bookmaker.setKey(t.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", ""));
                return bookmaker;
            });
        }
        return new ArrayList<>(map.values());
    }

    private PlusEvPickDto toDto(OddsOutcomeSnapshot snapshot, double bankrollDollars, double kellyFraction) {
        if (snapshot.getFairProbability() == null || snapshot.getBestDecimal() == null) {
            return null;
        }

        double fairProb = snapshot.getFairProbability();
        double offeredDecimal = snapshot.getBestDecimal();
        double edge = snapshot.getEdge() != null ? snapshot.getEdge() : fairProb * offeredDecimal - 1.0;
        if (!Double.isFinite(edge) || edge <= 0) {
            return null;
        }

        double stake = kellyStake(bankrollDollars, kellyFraction, fairProb, offeredDecimal);
        double fairDecimal = snapshot.getFairDecimal() != null
                ? snapshot.getFairDecimal()
                : (fairProb > 0 ? 1.0 / fairProb : 0.0);

        return PlusEvPickDto.builder()
                .formattedCommenceTime(formatCommence(snapshot.getEventCommence()))
                .category(Optional.ofNullable(snapshot.getSportTitle()).orElse(snapshot.getSportKey()))
                .sportName(snapshot.getSportTitle())
                .homeTeam(snapshot.getHomeTeam())
                .awayTeam(snapshot.getAwayTeam())
                .market(snapshot.getMarketType())
                .outcome(snapshot.getOutcomeKey())
                .fairProb(fairProb)
                .fairDecimal(round2(fairDecimal))
                .bookmakerTitle(snapshot.getBookmakerTitle())
                .offeredAmerican(snapshot.getBestAmerican() != null ? snapshot.getBestAmerican() : 0)
                .offeredDecimal(round2(offeredDecimal))
                .edgePct(round2(edge * 100.0))
                .recommendedStakeDollars(round2(stake))
                .build();
    }

    private static String formatCommence(Instant commence) {
        if (commence == null) {
            return "TBD";
        }
        return DISPLAY_FORMAT.format(commence);
    }

    private static double kellyStake(double bankroll, double kellyFraction, double p, double d) {
        double b = d - 1.0;
        double q = 1.0 - p;
        double fStar = (b * p - q) / b;
        double f = Math.max(0.0, fStar) * kellyFraction;
        return bankroll * f;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
