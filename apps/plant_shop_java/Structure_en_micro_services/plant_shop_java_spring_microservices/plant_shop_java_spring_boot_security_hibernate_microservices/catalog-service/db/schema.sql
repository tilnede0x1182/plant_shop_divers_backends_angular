-- ───────────────────────────────
--   Catalog Service - Plants Table
-- ───────────────────────────────

CREATE TABLE IF NOT EXISTS plants (
	id SERIAL PRIMARY KEY,
	name VARCHAR(100) NOT NULL,
	description TEXT,
	price NUMERIC(10,2) NOT NULL,
	stock INTEGER NOT NULL DEFAULT 0,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
