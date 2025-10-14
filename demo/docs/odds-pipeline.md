# Odds Processing and Persistence Strategy

This document outlines a recommended strategy for calculating, persisting, and querying
sportsbook odds data when refreshes arrive every 1–5 minutes.

## Guiding Principles

1. **Single pass computations** – Derive fair odds, best prices, and EV metrics at the
   time the market snapshot is ingested so downstream consumers read fully prepared
   data.
2. **Immutable snapshots** – Treat each refresh as an immutable snapshot, enabling
   easy auditing, trend analysis, and reproducible views.
3. **Targeted refresh windows** – Keep only the most recent snapshots in hot storage
   while archiving or expiring older data on a schedule that matches business needs.
4. **Queryable projections** – Expose focused database views or materialized queries to
   make page rendering simple and efficient.

## Processing Pipeline

1. **Fetch external odds data**
   * Pull raw odds from partner APIs and normalize them into a consistent domain model
     (`OddsSnapshot`, `OddsOutcome`). Include all fields needed to compute fair prices
     and EV metrics.

2. **Compute analytics in-memory**
   * Use a dedicated service (e.g., `OddsAnalyticsService`) that, for each matchup and
     outcome:
     - Calculates consensus probability (`fairProbability`) and fair price (decimal &
       American).
     - Determines the best available bookmaker price and its metadata.
     - Computes derived metrics such as edge, implied hold, and recommended stake.
   * Store the results in a rich DTO/entity (`PricedOutcome`) so every downstream
     consumer gets the same canonical values.

3. **Persist a fully populated record**
   * Persist the enriched `PricedOutcome` rows in a table keyed by `event_id`,
     `market_key`, and `outcome`. Suggested columns include:
     - Raw book odds by bookmaker (JSONB or child table if necessary).
     - Computed fair probability and prices.
     - Selected "best" bookmaker, its price, and the calculated EV/edge.
     - Snapshot metadata (`fetched_at`, `expires_at`).
   * Use an upsert keyed on the snapshot timestamp to avoid redundant writes and to
     keep the latest data accessible.

4. **Cache for API usage (optional)**
   * When latency is critical, layer a short-lived cache (Redis/ConcurrentHashMap) on
     top of the database. Cache entries can be invalidated using the snapshot timestamp
     so viewers never see stale data for more than the fetch cadence.

## Query Strategies

* **Odds page** – Query the latest snapshot per event and market, ordering by
  `fetched_at DESC LIMIT 1`. Rendering logic needs only to map the DB row into the view
  model because all pricing math is already complete.
* **+EV page** – Filter where `edge > 0` (or whatever minimum threshold you apply) and
  sort by `edge DESC`. Optionally add bookmaker/exchange filters or sport filters with
  indexed columns.
* **Historical analytics** – Retain snapshots (full table or summary table) for trend
  graphs, closing line value tracking, or alert back-testing.

## Maintenance Considerations

* Schedule a cleanup job to delete or archive rows older than the horizon you care
  about (e.g., 48 hours) if storage is a concern.
* Keep compute stateless so the processing component can scale horizontally when
  refresh frequency increases.
* Monitor write throughput—refreshing every minute across many markets may favor batch
  inserts or asynchronous queue processing to smooth load.

By persisting the pre-computed analytics you eliminate duplicate calculations per page
render, simplify controller logic, and make the +EV page a straightforward database
query while staying within the 1–5 minute refresh cadence.
