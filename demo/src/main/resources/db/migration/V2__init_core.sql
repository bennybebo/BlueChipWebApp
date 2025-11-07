BEGIN;

-- ---------- Enums ----------
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'market_type_enum') THEN
    CREATE TYPE market_type_enum AS ENUM ('moneyline', 'spread', 'total');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'selection_key_enum') THEN
    CREATE TYPE selection_key_enum AS ENUM ('home', 'away', 'draw', 'over', 'under');
  END IF;
END$$;

-- ---------- Reference: Sportsbooks ----------
CREATE TABLE IF NOT EXISTS sportsbook (
  sportsbook_id      BIGSERIAL PRIMARY KEY,
  key                TEXT        NOT NULL UNIQUE,  -- e.g., 'draftkings'
  title              TEXT        NOT NULL,         -- e.g., 'DraftKings'
  logo_slug          TEXT        NULL              -- optional, for easy logo pathing
);

-- ---------- Reference: Events ----------
CREATE TABLE IF NOT EXISTS event (
  event_id           TEXT        PRIMARY KEY,      -- from provider, e.g., '13bdff...'
  sport_key          TEXT        NOT NULL,         -- e.g., 'americanfootball_nfl'
  sport_title        TEXT        NOT NULL,         -- e.g., 'NFL'
  commence_time_utc  TIMESTAMPTZ NOT NULL,
  home_team          TEXT        NOT NULL,
  away_team          TEXT        NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_event_sport_time
  ON event (sport_key, commence_time_utc);

-- ---------- Snapshot boundary ----------
CREATE TABLE IF NOT EXISTS snapshot (
  snapshot_id        BIGSERIAL   PRIMARY KEY,
  as_of              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Optional: a single-row pointer if you prefer not to SELECT max(snapshot_id)
CREATE TABLE IF NOT EXISTS snapshot_meta (
  id                 SMALLINT    PRIMARY KEY DEFAULT 1 CHECK (id = 1),
  latest_snapshot_id BIGINT      NULL REFERENCES snapshot(snapshot_id) ON DELETE SET NULL
);
INSERT INTO snapshot_meta (id) VALUES (1)
ON CONFLICT (id) DO NOTHING;

-- ---------- Current Prices (all books, raw per-selection quotes) ----------
-- One row per (snapshot × event × market_type × line_value × selection × sportsbook)
CREATE TABLE IF NOT EXISTS price_current (
  snapshot_id        BIGINT             NOT NULL REFERENCES snapshot(snapshot_id) ON DELETE CASCADE,
  event_id           TEXT               NOT NULL REFERENCES event(event_id)     ON DELETE CASCADE,
  market_type        market_type_enum   NOT NULL,   -- 'moneyline' | 'spread' | 'total'
  line_value         NUMERIC(7,3),                 -- NULL for moneyline; e.g., -3.5, 42.5
  selection_key      selection_key_enum NOT NULL,   -- 'home'|'away'|'draw'|'over'|'under'
  sportsbook_id      BIGINT             NOT NULL REFERENCES sportsbook(sportsbook_id),

  american_odds      INT,                           -- optional to store
  decimal_odds       NUMERIC(8,3)       NOT NULL,
  last_update_utc    TIMESTAMPTZ,
  raw_json           JSONB,                         -- optional: store provider slice for audit

  PRIMARY KEY (snapshot_id, event_id, market_type, line_value, selection_key, sportsbook_id)
);

CREATE INDEX IF NOT EXISTS idx_price_event_mkt_line
  ON price_current (event_id, market_type, line_value);

CREATE INDEX IF NOT EXISTS idx_price_sportsbook
  ON price_current (sportsbook_id);

-- ---------- Precomputed (global) Derived Metrics ----------
-- One row per (snapshot × event × market_type × line_value × selection)
-- Stores global best line/EV/fair values for a selection at a specific line (or NULL line for moneyline)
CREATE TABLE IF NOT EXISTS derived_current (
  snapshot_id        BIGINT             NOT NULL REFERENCES snapshot(snapshot_id) ON DELETE CASCADE,
  event_id           TEXT               NOT NULL REFERENCES event(event_id)       ON DELETE CASCADE,
  market_type        market_type_enum   NOT NULL,
  line_value         NUMERIC(7,3),                 -- NULL for moneyline
  selection_key      selection_key_enum NOT NULL,

  -- Best odds across all books (global board)
  best_decimal_odds  NUMERIC(8,3)       NOT NULL,
  best_american_odds INT,
  best_sportsbook_id BIGINT             NOT NULL REFERENCES sportsbook(sportsbook_id),

  -- Fair line / odds (precomputed, no-vig)
  fair_decimal_odds  NUMERIC(8,3),
  fair_american_odds INT,
  fair_prob          NUMERIC(8,5),                 -- 0..1, optional if you prefer probability

  -- Expected value at best odds (percentage points, e.g., 3.2 for +3.2%)
  ev_percent_best    NUMERIC(6,3),

  PRIMARY KEY (snapshot_id, event_id, market_type, line_value, selection_key)
);

CREATE INDEX IF NOT EXISTS idx_derived_event_mkt_line
  ON derived_current (event_id, market_type, line_value);

CREATE INDEX IF NOT EXISTS idx_derived_ev_desc
  ON derived_current (ev_percent_best DESC);

-- ---------- Convenience Views ----------
-- Always reads from the single latest snapshot.
-- You can switch between MAX(snapshot_id) or snapshot_meta.latest_snapshot_id below.

CREATE OR REPLACE VIEW v_latest_snapshot AS
SELECT COALESCE(
  (SELECT latest_snapshot_id FROM snapshot_meta WHERE id = 1),
  (SELECT MAX(snapshot_id) FROM snapshot)
) AS snapshot_id;

CREATE OR REPLACE VIEW v_price_latest AS
SELECT pc.*
FROM price_current pc
JOIN v_latest_snapshot vls ON pc.snapshot_id = vls.snapshot_id;

CREATE OR REPLACE VIEW v_derived_latest AS
SELECT d.*, e.sport_key, e.sport_title, e.commence_time_utc, e.home_team, e.away_team
FROM derived_current d
JOIN v_latest_snapshot vls ON d.snapshot_id = vls.snapshot_id
JOIN event e ON e.event_id = d.event_id;

-- Optional helper for quickly listing active sportsbooks for the latest snapshot/sport
CREATE OR REPLACE VIEW v_active_sportsbooks_latest AS
SELECT DISTINCT s.sportsbook_id, s.key, s.title, s.logo_slug
FROM price_current pc
JOIN v_latest_snapshot vls ON pc.snapshot_id = vls.snapshot_id
JOIN sportsbook s ON s.sportsbook_id = pc.sportsbook_id;

COMMIT;
