-- Note: we use 'app_user' (not 'user') since USER is a reserved word in Postgres.
CREATE TABLE IF NOT EXISTS app_user (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(100) NOT NULL,
  roles VARCHAR(200) NOT NULL DEFAULT 'ROLE_USER',
  bankroll_cents BIGINT NOT NULL DEFAULT 0,
  preferred_kelly_fraction DOUBLE PRECISION NOT NULL DEFAULT 0.5,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

