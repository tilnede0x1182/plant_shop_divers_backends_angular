## ────────────────
# Configuration
## ────────────────

STACK = stack
APP = plant-shop-haskell
DB = plant_shop_haskell

## ────────────────
# Server
## ────────────────

build:
	$(STACK) build

run:
	$(STACK) run

prod: run

build-run: build run

clean:
	$(STACK) clean

build-dev: clean build-run

## ────────────────
# Seed
## ────────────────

seed:
	$(STACK) runhaskell src/db/Seed.hs

## ────────────────
# DB
## ────────────────

db-create:
	psql -U tilnede0x1182 -d postgres -c "CREATE DATABASE $(DB);"

db-migrate:
	psql -U tilnede0x1182 -d $(DB) -f src/db/schema.sql

db-drop:
	psql -U tilnede0x1182 -d postgres -c "DROP DATABASE IF EXISTS $(DB);"

db-reset: db-drop db-create seed

## ────────────────
# Test
## ────────────────

tests:
	$(STACK) runhaskell test/Test_e2e.hs

# ───────────────────────────────
#   Utilitaires
# ───────────────────────────────

tree:
	tree -I ".stack-work"
