# Variables de base de données (à extraire du .env)
DB_USER = tilnede0x1182
DB_PASS = tilnede0x1182
DB_HOST = localhost
DB_PORT = 5432
DB_NAME = plant_shop_manifest

patch-manifest:
	./patch_manifest/patch_manifest.sh

# run: patch-manifest
# 	npm run dev

prod: run

build: run

seed: patch-manifest
	npm run seed
# 	npx dotenv -e .env -- node ./db/seed-custom.js

proxy:
	node proxy.js

PM2_CTL = node ./pm2_custom.js

pm2-start-server: patch-manifest
	$(PM2_CTL) start server

pm2-stop-server:
	$(PM2_CTL) stop server

pm2-stop-proxy:
	$(PM2_CTL) stop proxy

run: pm2-start-all

pm2-start-all: patch-manifest
	$(PM2_CTL) start all

pm2-stop-all:
	$(PM2_CTL) stop all

# Créer la base de données
db-create:
	@echo "📦 Création de la base de données $(DB_NAME)..."
	@PGPASSWORD=$(DB_PASS) psql -U $(DB_USER) -h $(DB_HOST) -p $(DB_PORT) -d postgres -c "CREATE DATABASE $(DB_NAME);" 2>/dev/null || echo "⚠️  Base $(DB_NAME) existe déjà"

# Exécuter les migrations
db-migrate:
	@echo "🔄 Exécution des migrations..."
	@PGPASSWORD=$(DB_PASS) psql -U $(DB_USER) -h $(DB_HOST) -p $(DB_PORT) -d $(DB_NAME) -f ./db/migrate.sql
	@echo "✅ Migrations terminées"

# Supprimer la base de données
db-drop:
	@echo "🗑️  Suppression de la base de données $(DB_NAME)..."
	@PGPASSWORD=$(DB_PASS) psql -U $(DB_USER) -h $(DB_HOST) -p $(DB_PORT) -d postgres -c "DROP DATABASE IF EXISTS $(DB_NAME);"
	@echo "✅ Base supprimée"

# Reset complet: drop + create + migrate
db-reset: db-drop db-create db-migrate seed
	@echo "🔄 Reset de la base de données terminé"
