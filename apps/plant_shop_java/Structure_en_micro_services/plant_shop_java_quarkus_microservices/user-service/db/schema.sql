-- ───────────────────────────────
--   User Service - Users Table
-- ───────────────────────────────

CREATE TABLE IF NOT EXISTS users (
	id SERIAL PRIMARY KEY,
	email VARCHAR(255) UNIQUE NOT NULL,
	name VARCHAR(64) NOT NULL,
	password_hash VARCHAR(255) NOT NULL,
	is_admin BOOLEAN NOT NULL DEFAULT FALSE,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
