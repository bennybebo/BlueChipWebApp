package com.bluechip.demo.controller;

import com.bluechip.demo.model.*;
import com.bluechip.demo.repositories.UserRepository;
import com.bluechip.demo.service.BestOddsService;
import com.bluechip.demo.service.FairPriceService;
import com.bluechip.demo.service.OddsService;
import com.bluechip.demo.util.Utilities;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/odds")
public class OddsController {

    @Autowired private OddsService oddsService;
    @Autowired private BestOddsService bestOddsService;
    @Autowired private FairPriceService fairPriceService;
    @Autowired private JdbcTemplate jdbc;

    private final UserRepository userRepository;
    public OddsController(UserRepository userRepository) { this.userRepository = userRepository; }

    @GetMapping("/{sport}/{market}")
    public String getOdds(
            @PathVariable("sport") String sportKey,
            @PathVariable("market") String marketType,
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "false") boolean refresh, // <-- manual trigger
            Model model,
            HttpServletRequest request) {

        request.getSession(); // keep session alive

        try {
            if (refresh) {
                oddsService.refreshSportSnapshot(sportKey, marketType);
            }

            // 1) Read latest snapshot events for this sport+market
            List<Odds> oddsList = fetchOddsFromDb(sportKey, marketType);
            oddsList.sort(Comparator.comparing(Odds::getCommenceTime));

            // 2) Compute best odds for UI
            bestOddsService.computeBestOdds(oddsList);

            // Compute fair prices per event
            fairPriceService.computeFairPrice(oddsList, marketType);

            // 3) Collect unique bookmakers
            Set<Bookmaker> uniqueBookmakers = oddsList.stream()
                    .flatMap(o -> Optional.ofNullable(o.getBookmakers()).orElse(List.of()).stream())
                    .collect(Collectors.toMap(Bookmaker::getTitle, b -> b, (a, b) -> a))
                    .values()
                    .stream()
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // 4) Build matchup-to-bookmaker map for the view
            Map<String, Map<String, Bookmaker>> matchupBookmakerMap =
                    buildMatchupBookmakerMap(oddsList, marketType);

            // 5) Load sport name and available sports
            String sportName = Utilities.getSportName(sportKey);

            // 7) Determine user role
            boolean isPremium = false;
            if (principal != null) {
                User u = userRepository.findByUsername(principal.getUsername());
                isPremium = (u != null && u.getRoles() != null && u.getRoles().contains("ROLE_PREMIUM"));
            }

            // 8) Add everything to model
            model.addAttribute("oddsList", oddsList);
            model.addAttribute("uniqueBookmakers", uniqueBookmakers);
            model.addAttribute("matchupBookmakerMap", matchupBookmakerMap);
            model.addAttribute("sportKey", sportKey);
            model.addAttribute("marketType", marketType);
            model.addAttribute("sportName", sportName);
            model.addAttribute("availableSports", Utilities.AVAILABLE_SPORTS);
            model.addAttribute("isPremium", isPremium);
            model.addAttribute("refreshMode", refresh); // optional flag for UI feedback

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Unable to load odds data.");
        }

        return "odds_" + marketType;
    }


    /* ============================
       DB READS
       ============================ */
    private List<Odds> fetchOddsFromDb(String sportKey, String marketType) {
        String marketTypeEnum = marketType == null ? "" : marketType.toLowerCase();

        // 1) Get the events for the latest snapshot where this market exists
        List<Odds> events = jdbc.query("""
            SELECT DISTINCT e.event_id, e.sport_key, e.sport_title, e.commence_time_utc, e.home_team, e.away_team
            FROM v_price_latest pc
            JOIN event e ON e.event_id = pc.event_id
            WHERE e.sport_key = ? AND pc.market_type = ?::market_type_enum
        """, new Object[]{ sportKey, marketTypeEnum }, new OddsRowMapper());

        if (events.isEmpty()) return events;

        // 2) Load per-book quotes for those events
        String inIds = events.stream().map(Odds::getId).collect(Collectors.joining("','", "'", "'"));
        List<PriceRow> rows = jdbc.query("""
            SELECT pc.event_id,
                   pc.market_type::text AS market_type,
                   pc.line_value,
                   pc.selection_key::text AS selection_key,
                   pc.american_odds,
                   pc.decimal_odds,
                   s.key AS sb_key,
                   s.title AS sb_title
            FROM v_price_latest pc
            JOIN sportsbook s ON s.sportsbook_id = pc.sportsbook_id
            JOIN event e ON e.event_id = pc.event_id
            WHERE e.sport_key = ?
              AND pc.market_type = ?::market_type_enum
              AND pc.event_id IN (""" + inIds + ")",
            new Object[]{ sportKey, marketTypeEnum },
            (rs, i) -> new PriceRow(
                    rs.getString("event_id"),
                    rs.getString("market_type"),
                    (BigDecimal) rs.getObject("line_value"),
                    rs.getString("selection_key"),
                    (Integer) rs.getObject("american_odds"),
                    (BigDecimal) rs.getObject("decimal_odds"),
                    rs.getString("sb_key"),
                    rs.getString("sb_title")
            )
        );

        // 3) Group price rows into Bookmaker → Market → Outcomes per event
        Map<String, List<PriceRow>> byEvent = rows.stream().collect(Collectors.groupingBy(PriceRow::getEventId));
        Map<String, Odds> byEventOdds = events.stream().collect(Collectors.toMap(Odds::getId, o -> o));

        for (Map.Entry<String, List<PriceRow>> e : byEvent.entrySet()) {
            String eventId = e.getKey();
            Odds odds = byEventOdds.get(eventId);
            if (odds == null) continue;

            Map<String, Bookmaker> bookByTitle = new LinkedHashMap<>();
            for (PriceRow pr : e.getValue()) {
                Bookmaker book = bookByTitle.computeIfAbsent(pr.getSbTitle(), t -> {
                    Bookmaker b = new Bookmaker();
                    b.setKey(pr.getSbKey());
                    b.setTitle(pr.getSbTitle());
                    b.setMarkets(new ArrayList<>());
                    return b;
                });

                // Single market per view row
                Market mkt = ensureSingleMarket(book, marketType);

                // Create outcome label like the API provided
                Outcome out = new Outcome();
                if ("h2h".equals(marketTypeEnum)) {
                    // selection_key: home/away/draw → map to team names or 'Draw'
                    String sel = pr.getSelectionKey();
                    if ("home".equals(sel)) {
                        out.setName(odds.getHomeTeam());
                    } else if ("away".equals(sel)) {
                        out.setName(odds.getAwayTeam());
                    } else if ("draw".equals(sel)) {
                        out.setName("Draw");
                    } else {
                        continue;
                    }
                    out.setPoint(null);
                } else if ("totals".equals(marketTypeEnum)) {
                    out.setName(capitalize(pr.getSelectionKey())); // Over/Under
                    out.setPoint(toDouble(pr.getLineValue()));
                } else if ("spreads".equals(marketTypeEnum)) {
                    String sel = pr.getSelectionKey();
                    if ("home".equals(sel)) {
                        out.setName(odds.getHomeTeam());
                    } else if ("away".equals(sel)) {
                        out.setName(odds.getAwayTeam());
                    } else {
                        continue;
                    }
                    out.setPoint(toDouble(pr.getLineValue()));
                }

                out.setPrice(pr.getAmericanOdds()); // American odds for your template

                // Add to market
                List<Outcome> outs = mkt.getOutcomes();
                if (outs == null) {
                    outs = new ArrayList<>();
                    mkt.setOutcomes(outs);
                }
                outs.add(out);
            }

            odds.setBookmakers(new ArrayList<>(bookByTitle.values()));
        }

        return events;
    }

    private Market ensureSingleMarket(Bookmaker book, String marketType) {
        if (book.getMarkets() != null) {
            for (Market m : book.getMarkets()) {
                if (marketType.equalsIgnoreCase(m.getKey())) return m;
            }
        } else {
            book.setMarkets(new ArrayList<>());
        }
        Market m = new Market();
        m.setKey(marketType); // keep UI key to match your template filters
        m.setOutcomes(new ArrayList<>());
        book.getMarkets().add(m);
        return m;
    }

    private Map<String, Map<String, Bookmaker>> buildMatchupBookmakerMap(List<Odds> oddsList, String marketType) {
        Map<String, Map<String, Bookmaker>> map = new LinkedHashMap<>();
        for (Odds o : oddsList) {
            String matchup = o.getHomeTeam() + " vs " + o.getAwayTeam();
            Map<String, Bookmaker> inner = new LinkedHashMap<>();
            if (o.getBookmakers() != null) {
                for (Bookmaker b : o.getBookmakers()) {
                    boolean offers = b.getMarkets() != null && b.getMarkets().stream().anyMatch(m -> marketType.equalsIgnoreCase(m.getKey()));
                    if (offers) inner.put(b.getTitle(), b);
                }
            }
            map.put(matchup, inner);
        }
        return map;
    }

    /* ============================
       Row mappers / simple beans
       ============================ */

    private static class OddsRowMapper implements RowMapper<Odds> {
    @Override
    public Odds mapRow(ResultSet rs, int rowNum) throws SQLException {
            Odds o = new Odds();
            o.setId(rs.getString("event_id"));
            o.setSportKey(rs.getString("sport_key"));
            o.setSportTitle(rs.getString("sport_title"));

            OffsetDateTime odt = rs.getObject("commence_time_utc", OffsetDateTime.class);
            if (odt != null) {
                // Store as an ISO 8601 string to match Odds’ existing logic
                o.setCommenceTime(odt.toInstant());
            } else {
                o.setCommenceTime(null);
            }

            o.setHomeTeam(rs.getString("home_team"));
            o.setAwayTeam(rs.getString("away_team"));
            return o;
        }
    }


    private static class PriceRow {
        private final String eventId;
        private final String marketType;
        private final BigDecimal lineValue;
        private final String selectionKey;
        private final Integer americanOdds;
        private final BigDecimal decimalOdds;
        private final String sbKey;
        private final String sbTitle;

        public PriceRow(String eventId, String marketType, BigDecimal lineValue, String selectionKey,
                        Integer americanOdds, BigDecimal decimalOdds, String sbKey, String sbTitle) {
            this.eventId = eventId;
            this.marketType = marketType;
            this.lineValue = lineValue;
            this.selectionKey = selectionKey;
            this.americanOdds = americanOdds;
            this.decimalOdds = decimalOdds;
            this.sbKey = sbKey;
            this.sbTitle = sbTitle;
        }

        public String getEventId() { return eventId; }
        public String getMarketType() { return marketType; }
        public BigDecimal getLineValue() { return lineValue; }
        public String getSelectionKey() { return selectionKey; }
        public Integer getAmericanOdds() { return americanOdds; }
        public BigDecimal getDecimalOdds() { return decimalOdds; }
        public String getSbKey() { return sbKey; }
        public String getSbTitle() { return sbTitle; }
    }

    private static Double toDouble(BigDecimal v) { return v == null ? null : v.doubleValue(); }
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase() + s.substring(1).toLowerCase();
    }

    @PostMapping("/{sport}/{market}/refresh")
    public String refreshNow(@PathVariable("sport") String sportKey,
                         @PathVariable("market") String marketType,
                         RedirectAttributes ra) {
        try {
            oddsService.refreshSportSnapshot(sportKey, marketType);
            ra.addFlashAttribute("msg", "Refreshed!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Refresh failed: " + e.getMessage());
        }
        return "redirect:/odds/" + sportKey + "/" + marketType;
    }
}

