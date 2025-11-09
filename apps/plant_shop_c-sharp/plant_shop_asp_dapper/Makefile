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
	ASPNETCORE_ENVIRONMENT=Production dotnet ./bin/publish/plant_shop_asp_dapper.dll

clean:
	rm -rf bin obj

build-dev: clean compile_run

## ────────────────
# Seed
## ────────────────

compile-seed:
	dotnet build Data

seed:
	dotnet run --project Data

seed-dev: clean compile-seed seed

## ────────────────
# DB
## ────────────────

DB_NAME = plant_shop_asp_dapper

db-create:
	sudo -u postgres psql -c "CREATE DATABASE \"$(DB_NAME)\";"

db-migrate:
	sudo -u postgres psql -d "$(DB_NAME)" -f Data/schema.sql

db-drop:
	sudo -u postgres psql -c "DROP DATABASE IF EXISTS \"$(DB_NAME)\";"

db-seed: seed

db-reset: db-drop db-create db-migrate db-seed

## ────────────────
# Test
## ────────────────

compile-test:
	dotnet build test

tests:
	dotnet run --project test
