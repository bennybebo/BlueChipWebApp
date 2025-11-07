package com.bluechip.demo.service;

import com.bluechip.demo.model.Bookmaker;
import com.bluechip.demo.model.Market;
import com.bluechip.demo.model.Odds;
import com.bluechip.demo.model.Outcome;
import com.bluechip.demo.repositories.OddsRepository;
import com.bluechip.demo.util.Utilities;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OddsService {

    private final ObjectMapper objectMapper;
    private final OddsApiService oddsApiService;           // your existing API client
    private final OddsRepository repo;                     // the repository we just created

    /**
     * Fetches a sport/market from the aggregator, loads it into Postgres as a new snapshot,
     * precomputes GLOBAL BEST per (event, market, line, selection), and promotes the snapshot.
     * @param sportKey e.g., "americanfootball_nfl"
     * @param marketTypeUi one of "h2h" | "spreads" | "totals" (your current UI keys)
     * @return the promoted snapshot_id
     */
    @Transactional
    public long refreshSportSnapshot(String sportKey, String marketTypeUi) throws IOException {
        // 1) Fetch JSON
        String json = oddsApiService.fetchOddsForSportAndMarket(sportKey, marketTypeUi);

        // 2) Parse to DTO
        List<Odds> oddsList = objectMapper.readValue(json, new TypeReference<List<Odds>>() {});

        // 3) Snapshot
        long snapshotId = repo.beginSnapshot(Instant.now());

        // 4) Sportsbooks
        List<OddsRepository.SportsbookRef> books = extractSportsbooks(oddsList);
        Map<String, Long> bookIdByKey = repo.upsertSportsbooks(books);

        // 5) Events
        List<OddsRepository.EventRow> events = extractEvents(oddsList);
        repo.upsertEvents(events);

        // 6) Prices
        OddsRepository.MarketType marketType = mapUiMarketToEnum(marketTypeUi);
        List<OddsRepository.PriceRow> priceRows = buildPriceRows(oddsList, marketTypeUi, marketType, bookIdByKey);
        repo.insertPriceBatch(snapshotId, priceRows);

        // 7) Best rows (from market board)
        List<OddsRepository.DerivedRow> bestRows = buildDerivedBestRows(priceRows);

        // TODO: HANDLE FAIR PRICES. Possibly in buildDerivedBestRows or a new method?

        // 10) Persist derived
        repo.insertDerivedBatch(snapshotId, bestRows);

        // 11) Promote
        repo.promoteSnapshot(snapshotId);

        return snapshotId;
    }

    private List<OddsRepository.SportsbookRef> extractSportsbooks(List<Odds> oddsList) {
        Map<String, OddsRepository.SportsbookRef> uniq = new HashMap<>();
        for (Odds o : oddsList) {
            if (o.getBookmakers() == null) continue;
            for (Bookmaker b : o.getBookmakers()) {
                if (b.getKey() == null) continue;
                String key = b.getKey();
                String title = b.getTitle() != null ? b.getTitle() : key;
                String logoSlug = toLogoSlug(title);
                uniq.putIfAbsent(key, new OddsRepository.SportsbookRef(key, title, logoSlug));
            }
        }
        return new ArrayList<>(uniq.values());
    }

    private List<OddsRepository.EventRow> extractEvents(List<Odds> oddsList) {
        return oddsList.stream().map(o ->
                new OddsRepository.EventRow(
                        o.getId(),
                        o.getSportKey(),
                        o.getSportTitle(),
                        o.getCommenceTime(), // assuming Instant (or convert)
                        o.getHomeTeam(),
                        o.getAwayTeam()
                )
        ).toList();
    }

    private List<OddsRepository.PriceRow> buildPriceRows(
            List<Odds> oddsList,
            String marketTypeUi,
            OddsRepository.MarketType marketType,
            Map<String, Long> bookIdByKey
    ) {
        List<OddsRepository.PriceRow> rows = new ArrayList<>();

        for (Odds o : oddsList) {
            String eventId = o.getId();
            String home = o.getHomeTeam();
            String away = o.getAwayTeam();

            if (o.getBookmakers() == null) continue;
            for (Bookmaker b : o.getBookmakers()) {
                Long sbId = bookIdByKey.get(b.getKey());
                if (sbId == null) continue;

                Instant lastUpdateBook = b.getLastUpdate();

                if (b.getMarkets() == null) continue;
                for (Market m : b.getMarkets()) {
                    if (!marketTypeUi.equalsIgnoreCase(m.getKey())) continue;

                    Instant lastUpdate = m.getLastUpdate() != null ? m.getLastUpdate() : lastUpdateBook;

                    if (m.getOutcomes() == null) continue;
                    for (Outcome out : m.getOutcomes()) {
                        Mapping mapped = mapOutcomeToSchema(marketType, out, home, away);
                        if (mapped == null) continue;

                        BigDecimal dec = Utilities.americanToDecimal(out.getPrice());
                        rows.add(new OddsRepository.PriceRow(
                                eventId,
                                marketType,
                                mapped.lineValueNullable,
                                mapped.selection,
                                sbId,
                                out.getPrice(),                // american
                                dec,                           // decimal
                                lastUpdate,
                                null                           // raw_json (optional)
                        ));
                    }
                }
            }
        }

        return rows;
    }

    /** Reduce all price rows to GLOBAL best per (event, market, line, selection). */
    private List<OddsRepository.DerivedRow> buildDerivedBestRows(List<OddsRepository.PriceRow> priceRows) {
        record Key(String event, OddsRepository.MarketType mt, BigDecimal line, OddsRepository.SelectionKey sel) {}

        Map<Key, OddsRepository.PriceRow> best = new HashMap<>();
        for (OddsRepository.PriceRow r : priceRows) {
            Key k = new Key(r.eventId(), r.marketType(), r.lineValueNullable(), r.selection());
            OddsRepository.PriceRow current = best.get(k);
            if (current == null || r.decimalOdds().compareTo(current.decimalOdds()) > 0) {
                best.put(k, r);
            }
        }

        List<OddsRepository.DerivedRow> rows = new ArrayList<>(best.size());
        for (Map.Entry<Key, OddsRepository.PriceRow> e : best.entrySet()) {
            Key k = e.getKey();
            OddsRepository.PriceRow r = e.getValue();
            rows.add(new OddsRepository.DerivedRow(
                    k.event,
                    k.mt,
                    k.line,
                    k.sel,
                    r.decimalOdds(),          // best_decimal
                    r.americanOddsNullable(), // best_american
                    r.sportsbookId(),         // best_sportsbook_id
                    // TODO: fair prices and ev% to be filled later
                    null,                     // fair_decimal (precompute later)
                    null,                     // fair_american
                    null,                     // fair_prob
                    null                      // ev_percent_best
            ));
        }
        return rows;
    }

    
    private OddsRepository.MarketType mapUiMarketToEnum(String ui) {
        if (ui == null) throw new IllegalArgumentException("marketTypeUi is null");

        return switch (ui.toLowerCase()) {
            case "h2h" -> OddsRepository.MarketType.h2h;
            case "spreads" -> OddsRepository.MarketType.spreads;
            case "totals" -> OddsRepository.MarketType.totals;
            default -> throw new IllegalArgumentException("Unknown market type UI: " + ui);
        };
    }

    /** Maps one API outcome into our (selection_key, line_value) for a given market type. */
    private Mapping mapOutcomeToSchema(OddsRepository.MarketType marketType, Outcome out, String homeTeam, String awayTeam) {
        if (out == null) return null;

        switch (marketType) {
            case h2h -> {
                String n = safe(out.getName());
                if (n.equalsIgnoreCase(safe(homeTeam))) {
                    return new Mapping(OddsRepository.SelectionKey.home, null);
                } else if (n.equalsIgnoreCase(safe(awayTeam))) {
                    return new Mapping(OddsRepository.SelectionKey.away, null);
                } else if (n.equalsIgnoreCase("draw")) {
                    return new Mapping(OddsRepository.SelectionKey.draw, null);
                }
                return null; // unknown label
            }
            case totals -> {
                String n = safe(out.getName());
                if (n.equalsIgnoreCase("over")) {
                    return new Mapping(OddsRepository.SelectionKey.over, toBig(out.getPoint()));
                } else if (n.equalsIgnoreCase("under")) {
                    return new Mapping(OddsRepository.SelectionKey.under, toBig(out.getPoint()));
                }
                return null;
            }
            case spreads -> {
                String n = safe(out.getName());
                if (n.equalsIgnoreCase(safe(homeTeam))) {
                    return new Mapping(OddsRepository.SelectionKey.home, toBig(out.getPoint()));
                } else if (n.equalsIgnoreCase(safe(awayTeam))) {
                    return new Mapping(OddsRepository.SelectionKey.away, toBig(out.getPoint()));
                }
                return null;
            }
        }
        return null;
    }

    private String toLogoSlug(String title) {
        if (title == null) return null;
        return title.replaceAll("[^A-Za-z0-9]", "");
    }

    private String safe(String s) { return s == null ? "" : s; }
    private BigDecimal toBig(Double d) { return d == null ? null : BigDecimal.valueOf(d).setScale(3, RoundingMode.HALF_UP); }

    /* small holder for mapping result */
    private record Mapping(OddsRepository.SelectionKey selection, BigDecimal lineValueNullable) {}
}