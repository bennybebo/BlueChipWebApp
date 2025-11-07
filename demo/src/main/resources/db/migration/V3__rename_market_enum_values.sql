-- Rename enum values to match The Odds API
ALTER TYPE market_type_enum RENAME VALUE 'moneyline' TO 'h2h';
ALTER TYPE market_type_enum RENAME VALUE 'spread'    TO 'spreads';
ALTER TYPE market_type_enum RENAME VALUE 'total'     TO 'totals';
