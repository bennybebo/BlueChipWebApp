package com.bluechip.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "odds_outcome_snapshot",
       uniqueConstraints = @UniqueConstraint(name = "uk_odds_snapshot_event_outcome",
            columnNames = {"sport_key", "market_type", "event_id", "outcome_key"}))
public class OddsOutcomeSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sport_key", nullable = false, length = 64)
    private String sportKey;

    @Column(name = "sport_title", length = 128)
    private String sportTitle;

    @Column(name = "market_type", nullable = false, length = 32)
    private String marketType;

    @Column(name = "event_id", length = 128)
    private String eventId;

    @Column(name = "event_commence")
    private Instant eventCommence;

    @Column(name = "home_team", length = 128)
    private String homeTeam;

    @Column(name = "away_team", length = 128)
    private String awayTeam;

    @Column(name = "outcome_key", nullable = false, length = 32)
    private String outcomeKey;

    @Column(name = "outcome_name", length = 128)
    private String outcomeName;

    @Column(name = "point")
    private Double point;

    @Column(name = "bookmaker_title", length = 128)
    private String bookmakerTitle;

    @Column(name = "best_american")
    private Integer bestAmerican;

    @Column(name = "best_decimal")
    private Double bestDecimal;

    @Column(name = "fair_probability")
    private Double fairProbability;

    @Column(name = "fair_decimal")
    private Double fairDecimal;

    @Column(name = "fair_american")
    private Integer fairAmerican;

    @Column(name = "edge")
    private Double edge;

    @Column(name = "refreshed_at", nullable = false)
    private Instant refreshedAt;
}
