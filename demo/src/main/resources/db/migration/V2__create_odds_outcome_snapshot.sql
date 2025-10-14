CREATE TABLE IF NOT EXISTS odds_outcome_snapshot (
    id BIGSERIAL PRIMARY KEY,
    sport_key VARCHAR(64) NOT NULL,
    sport_title VARCHAR(128),
    market_type VARCHAR(32) NOT NULL,
    event_id VARCHAR(128),
    event_commence TIMESTAMPTZ,
    home_team VARCHAR(128),
    away_team VARCHAR(128),
    outcome_key VARCHAR(32) NOT NULL,
    outcome_name VARCHAR(128),
    point DOUBLE PRECISION,
    bookmaker_title VARCHAR(128),
    best_american INTEGER,
    best_decimal DOUBLE PRECISION,
    fair_probability DOUBLE PRECISION,
    fair_decimal DOUBLE PRECISION,
    fair_american INTEGER,
    edge DOUBLE PRECISION,
    refreshed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_odds_snapshot_event_outcome
    ON odds_outcome_snapshot (sport_key, market_type, event_id, outcome_key);
