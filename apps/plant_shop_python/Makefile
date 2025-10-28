# ────────────────
# Server
# ────────────────

run:
	python3 -m backend_py.app

compile_run: run

prod: run

clean:
	find backend_py -type f -name "*.pyc" -delete
	find backend_py -type d -name "__pycache__" -exec rm -rf {} +

build-dev: clean compile_run

# ────────────────
# Seed
# ────────────────

seed:
	python3 db/seed.py

seed-dev: clean seed

# ────────────────
# DB
# ────────────────

db-create:
	sudo -u postgres psql -c "CREATE DATABASE plant_shop_python;"

db-migrate:
	sudo -u postgres psql -d plant_shop_python -f db/schema.sql

db-drop:
	sudo -u postgres psql -c "DROP DATABASE plant_shop_python;"

db-seed: seed

db-reset: db-drop db-create db-migrate db-seed

# ────────────────
# Test
# ────────────────

tests:
	python3 -m backend_py.tests.test_e2e
