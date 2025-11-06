# Variables de base de données (à extraire du .env)
DB_USER = tilnede0x1182
DB_PASS = tilnede0x1182
DB_HOST = localhost
DB_PORT = 5432
DB_NAME = plant_shop_manifest

patch-manifest:
	./patch_manifest/patch_manifest.sh

run: patch-manifest
	npm run dev

prod: patch-manifest
	npm run start

seed: patch-manifest
	npm run seed
# 	npx dotenv -e .env -- node ./db/seed-custom.js

proxy:
	node proxy.js

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
