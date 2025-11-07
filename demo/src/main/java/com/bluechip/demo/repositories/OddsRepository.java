package com.bluechip.demo.repositories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class OddsRepository {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final ObjectMapper objectMapper;

    public OddsRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbc = jdbcTemplate;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.objectMapper = objectMapper;
    }

    /** Inserts a new snapshot and returns its snapshot_id. (binds as JDBC Timestamp) */
    public long beginSnapshot(Instant asOf) {
        final String sql = "INSERT INTO snapshot (as_of) VALUES (?) RETURNING snapshot_id";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[] { "snapshot_id" });
            ps.setTimestamp(1, Timestamp.from(asOf));
            return ps;
        }, kh);
        Number key = kh.getKey();
        if (key == null) throw new IllegalStateException("Snapshot insert returned no key");
        return key.longValue();
    }

    /** Atomically promotes a snapshot to be the latest. */
    public void promoteSnapshot(long snapshotId) {
        jdbc.update("""
            INSERT INTO snapshot_meta (id, latest_snapshot_id)
            VALUES (1, ?)
            ON CONFLICT (id) DO UPDATE SET latest_snapshot_id = EXCLUDED.latest_snapshot_id
        """, snapshotId);
    }

    /** Upserts sportsbooks and returns a map: sportsbook.key -> sportsbook_id. */
    public Map<String, Long> upsertSportsbooks(Collection<SportsbookRef> books) {
        if (books == null || books.isEmpty()) return Collections.emptyMap();

        jdbc.batchUpdate("""
            INSERT INTO sportsbook (key, title, logo_slug)
            VALUES (?, ?, ?)
            ON CONFLICT (key) DO UPDATE
              SET title = EXCLUDED.title,
                  logo_slug = EXCLUDED.logo_slug
        """, books, 500, (PreparedStatement ps, SportsbookRef b) -> {
            ps.setString(1, b.key());
            ps.setString(2, b.title());
            if (b.logoSlug() == null) ps.setNull(3, Types.VARCHAR); else ps.setString(3, b.logoSlug());
        });

        var keys = books.stream().map(SportsbookRef::key).collect(Collectors.toSet());
        Map<String, Object> params = Map.of("keys", keys);
        return namedJdbc.query("""
                SELECT key, sportsbook_id
                FROM sportsbook
                WHERE key IN (:keys)
            """, params, rs -> {
            Map<String, Long> map = new HashMap<>();
            while (rs.next()) map.put(rs.getString("key"), rs.getLong("sportsbook_id"));
            return map;
        });
    }

    /** Upserts events (idempotent). */
    public void upsertEvents(Collection<EventRow> events) {
        if (events == null || events.isEmpty()) return;

        jdbc.batchUpdate("""
            INSERT INTO event (event_id, sport_key, sport_title, commence_time_utc, home_team, away_team)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (event_id) DO UPDATE
              SET sport_key = EXCLUDED.sport_key,
                  sport_title = EXCLUDED.sport_title,
                  commence_time_utc = EXCLUDED.commence_time_utc,
                  home_team = EXCLUDED.home_team,
                  away_team = EXCLUDED.away_team
        """, events, 500, (PreparedStatement ps, EventRow e) -> {
            ps.setString(1, e.eventId());
            ps.setString(2, e.sportKey());
            ps.setString(3, e.sportTitle());
            ps.setTimestamp(4, Timestamp.from(e.commenceTimeUtc())); // <-- key fix
            ps.setString(5, e.homeTeam());
            ps.setString(6, e.awayTeam());
        });
    }

    /** Inserts a batch of raw/current quotes for a given snapshot. */
    public void insertPriceBatch(long snapshotId, Collection<PriceRow> prices) {
        if (prices == null || prices.isEmpty()) return;

        jdbc.batchUpdate("""
            INSERT INTO price_current (
            snapshot_id, event_id, market_type, line_value, selection_key, sportsbook_id,
            american_odds, decimal_odds, last_update_utc, raw_json
            )
            VALUES (
            ?, ?, ?::market_type_enum, ?, ?::selection_key_enum, ?,
            ?, ?, ?, ?
            )
            ON CONFLICT (snapshot_id, event_id, market_type, line_value, selection_key, sportsbook_id)
            DO NOTHING
        """, prices, 1000, (PreparedStatement ps, PriceRow r) -> {
            ps.setLong(1, snapshotId);
            ps.setString(2, r.eventId());
            ps.setString(3, r.marketType().name());

            if (r.lineValueNullable() == null) {
                ps.setBigDecimal(4, BigDecimal.ZERO);
            } else {
                ps.setBigDecimal(4, r.lineValueNullable());
            }

            ps.setString(5, r.selection().name());
            ps.setLong(6, r.sportsbookId());

            if (r.americanOddsNullable() == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, r.americanOddsNullable());
            ps.setBigDecimal(8, r.decimalOdds());

            if (r.lastUpdateUtcNullable() == null) {
                ps.setNull(9, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(9, java.sql.Timestamp.from(r.lastUpdateUtcNullable()));
            }

            if (r.rawJsonNullable() == null) {
                ps.setNull(10, Types.OTHER);
            } else {
                ps.setObject(10, toPgJson(r.rawJsonNullable()));
            }
        });
    }


    /** Inserts a batch of precomputed derived rows for a given snapshot. */
    public void insertDerivedBatch(long snapshotId, Collection<DerivedRow> rows) {
        if (rows == null || rows.isEmpty()) return;

        jdbc.batchUpdate("""
            INSERT INTO derived_current (
            snapshot_id, event_id, market_type, line_value, selection_key,
            best_decimal_odds, best_american_odds, best_sportsbook_id,
            fair_decimal_odds, fair_american_odds, fair_prob,
            ev_percent_best
            )
            VALUES (
            ?, ?, ?::market_type_enum, ?, ?::selection_key_enum,
            ?, ?, ?,
            ?, ?, ?,
            ?
            )
            ON CONFLICT (snapshot_id, event_id, market_type, line_value, selection_key)
            DO UPDATE SET
            best_decimal_odds = EXCLUDED.best_decimal_odds,
            best_american_odds = EXCLUDED.best_american_odds,
            best_sportsbook_id = EXCLUDED.best_sportsbook_id,
            fair_decimal_odds = EXCLUDED.fair_decimal_odds,
            fair_american_odds = EXCLUDED.fair_american_odds,
            fair_prob         = EXCLUDED.fair_prob,
            ev_percent_best   = EXCLUDED.ev_percent_best
        """, rows, 1000, (PreparedStatement ps, DerivedRow r) -> {
            ps.setLong(1, snapshotId);
            ps.setString(2, r.eventId());
            ps.setString(3, r.marketType().name());

            // 👇 Force 0 for moneyline (null) lines
            if (r.lineValueNullable() == null) {
                ps.setBigDecimal(4, BigDecimal.ZERO);
            } else {
                ps.setBigDecimal(4, r.lineValueNullable());
            }

            ps.setString(5, r.selection().name());

            ps.setBigDecimal(6, r.bestDecimal());
            if (r.bestAmericanNullable() == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, r.bestAmericanNullable());
            ps.setLong(8, r.bestSportsbookId());

            if (r.fairDecimalNullable() == null) ps.setNull(9, Types.NUMERIC); else ps.setBigDecimal(9, r.fairDecimalNullable());
            if (r.fairAmericanNullable() == null) ps.setNull(10, Types.INTEGER); else ps.setInt(10, r.fairAmericanNullable());
            if (r.fairProbNullable() == null) ps.setNull(11, Types.NUMERIC); else ps.setBigDecimal(11, r.fairProbNullable());

            if (r.evPercentBestNullable() == null) ps.setNull(12, Types.NUMERIC); else ps.setBigDecimal(12, r.evPercentBestNullable());
        });
    }

    /* ============================
       Helpers
       ============================ */

    private PGobject toPgJson(JsonNode node) {
        try {
            PGobject o = new PGobject();
            o.setType("jsonb");
            o.setValue(objectMapper.writeValueAsString(node));
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to jsonb", e);
        }
    }

    /* ============================
       DTOs / Enums (kept here)
       ============================ */

    public enum MarketType { h2h, spreads, totals }
    public enum SelectionKey { home, away, draw, over, under }

    public record SportsbookRef(String key, String title, String logoSlug) {}

    public record EventRow(
            String eventId,
            String sportKey,
            String sportTitle,
            Instant commenceTimeUtc,
            String homeTeam,
            String awayTeam
    ) {}

    public record PriceRow(
            String eventId,
            MarketType marketType,
            BigDecimal lineValueNullable,
            SelectionKey selection,
            long sportsbookId,
            Integer americanOddsNullable,
            BigDecimal decimalOdds,
            Instant lastUpdateUtcNullable,
            JsonNode rawJsonNullable
    ) {}

    public record DerivedRow(
            String eventId,
            MarketType marketType,
            BigDecimal lineValueNullable,
            SelectionKey selection,
            BigDecimal bestDecimal,
            Integer bestAmericanNullable,
            long bestSportsbookId,
            BigDecimal fairDecimalNullable,
            Integer fairAmericanNullable,
            BigDecimal fairProbNullable,
            BigDecimal evPercentBestNullable
    ) {}
}
