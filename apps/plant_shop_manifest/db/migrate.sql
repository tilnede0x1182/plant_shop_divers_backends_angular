-- Migration SQL pour plant_shop_manifest
-- Création des tables basées sur manifest.yml (noms en minuscules utilisés par Manifest)

DROP TABLE IF EXISTS "order_item" CASCADE;
DROP TABLE IF EXISTS "order" CASCADE;
DROP TABLE IF EXISTS "plant" CASCADE;
DROP TABLE IF EXISTS "admin" CASCADE;
DROP TABLE IF EXISTS "user" CASCADE;

-- Table user
CREATE TABLE IF NOT EXISTS "user" (
    id SERIAL PRIMARY KEY,
    true_id BIGSERIAL UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    admin BOOLEAN DEFAULT FALSE,
    name VARCHAR(255),
    "createdAt" TIMESTAMP DEFAULT NOW(),
    "updatedAt" TIMESTAMP DEFAULT NOW()
);

-- Table plant
CREATE TABLE IF NOT EXISTS "plant" (
    id SERIAL PRIMARY KEY,
    true_id BIGSERIAL UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    price INTEGER NOT NULL,
    stock INTEGER NOT NULL,
    description TEXT,
    "createdAt" TIMESTAMP DEFAULT NOW(),
    "updatedAt" TIMESTAMP DEFAULT NOW()
);

-- Table order
CREATE TABLE IF NOT EXISTS "order" (
    id SERIAL PRIMARY KEY,
    true_id BIGSERIAL UNIQUE NOT NULL,
    "userId" INTEGER NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    "totalPrice" INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'pending',
    "createdAt" TIMESTAMP DEFAULT NOW(),
    "updatedAt" TIMESTAMP DEFAULT NOW()
);

-- Table order_item
CREATE TABLE IF NOT EXISTS "order_item" (
    id SERIAL PRIMARY KEY,
    true_id BIGSERIAL UNIQUE NOT NULL,
    "orderId" INTEGER NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    "plantId" INTEGER NOT NULL REFERENCES "plant"(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL,
    "createdAt" TIMESTAMP DEFAULT NOW(),
    "updatedAt" TIMESTAMP DEFAULT NOW()
);

-- Table admin (panel Manifest)
CREATE TABLE IF NOT EXISTS "admin" (
    id SERIAL PRIMARY KEY,
    true_id BIGSERIAL UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    "createdAt" TIMESTAMP DEFAULT NOW(),
    "updatedAt" TIMESTAMP DEFAULT NOW()
);

-- Index pour optimiser les requêtes
CREATE INDEX IF NOT EXISTS idx_user_email ON "user"(email);
CREATE INDEX IF NOT EXISTS idx_order_user ON "order"("userId");
CREATE INDEX IF NOT EXISTS idx_orderitem_order ON "order_item"("orderId");
CREATE INDEX IF NOT EXISTS idx_orderitem_plant ON "order_item"("plantId");
