# ────────────────
# Server
# ────────────────

run:
	python3 app.py

prod:
	gunicorn -w 1 -b 0.0.0.0:4100 app:app

clean:
	find backend_py -type f -name "*.pyc" -delete
	find backend_py -type d -name "__pycache__" -exec rm -rf {} +

# ────────────────
# Requirements
# ────────────────

install:
	pip install -r requirements.txt

# ────────────────
# Seed
# ────────────────

seed:
	python3 db/seed.py

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
	python3 test/test_e2e.py

# ───────────────────────────────
#   Utilitaires
# ───────────────────────────────

tree:
	tree -I "venv" -I "__pycache__"
