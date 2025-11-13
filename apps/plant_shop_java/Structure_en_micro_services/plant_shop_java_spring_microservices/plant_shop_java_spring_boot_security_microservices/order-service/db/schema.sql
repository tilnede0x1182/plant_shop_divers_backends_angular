-- ───────────────────────────────────────────────────────
--   Order Service - Orders, Order Items & Plant Stock
-- ───────────────────────────────────────────────────────

-- Table plant_stock pour l'indépendance du service
CREATE TABLE IF NOT EXISTS plant_stock (
	id INTEGER PRIMARY KEY,
	name VARCHAR(100) NOT NULL,
	price NUMERIC(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
	id SERIAL PRIMARY KEY,
	user_id INTEGER NOT NULL, -- Pas de FK vers users (autre service)
	total NUMERIC(10,2) NOT NULL,
	status VARCHAR(50) NOT NULL DEFAULT 'pending',
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_items (
	id SERIAL PRIMARY KEY,
	order_id INTEGER REFERENCES orders(id) ON DELETE CASCADE,
	plant_id INTEGER NOT NULL, -- Pas de FK vers plants (autre service)
	quantity INTEGER NOT NULL,
	price NUMERIC(10,2) NOT NULL
);

-- Index
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_plant_id ON order_items(plant_id);
