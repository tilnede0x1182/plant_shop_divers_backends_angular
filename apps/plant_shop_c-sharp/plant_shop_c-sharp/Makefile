## ────────────────
# Server
## ────────────────

compile:
	mkdir -p bin
	dotnet build

run:
	dotnet run

compile_run:
	dotnet run

prod:
	dotnet publish -c Release -o ./bin/publish
	dotnet ./bin/publish/plant_shop_c-sharp.dll

clean:
	rm -rf bin obj

build-dev: clean compile_run

## ────────────────
# Seed
## ────────────────

build-seed:
	dotnet build db/plant_shop_c-sharp.Seed.csproj

seed:
	dotnet run --project db/plant_shop_c-sharp.Seed.csproj

seed-dev: clean build-seed seed

## ────────────────
# DB
## ────────────────

DB_NAME = plant_shop_c-sharp

db-create:
	sudo -u postgres psql -c "CREATE DATABASE \"$(DB_NAME)\";"

db-migrate:
	sudo -u postgres psql -d "$(DB_NAME)" -f db/schema.sql

db-drop:
	sudo -u postgres psql -c "DROP DATABASE IF EXISTS \"$(DB_NAME)\";"

db-seed: seed

db-reset: db-drop db-create db-migrate db-seed

## ────────────────
# Test
## ────────────────

test-build:
	dotnet build test/plant_shop_c-sharp.Tests.csproj

tests:
	dotnet run --project test/plant_shop_c-sharp.Tests.csproj

test-dev: clean test-build tests
