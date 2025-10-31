# CLASSPATH est dynamiquement construit à partir des .jar dans /lib
CLASSPATH := bin:$(shell find lib -name "*.jar" | tr '\n' ':')
DB_NAME=plant_shop_java_micronaut

## ────────────────
# Dépendances
## ────────────────

# Ne pas toucher à cette section, elle gère le téléchargement des libs.
compile-install-libs:
	mkdir -p bin
	javac -d bin config/InstallLibs.java

install: compile-install-libs
	java -cp bin config/InstallLibs

## ────────────────
# Server
## ────────────────

compile:
	mkdir -p bin
	find src -type f -name "*.java" ! -path "*/db/Seed.java" ! -path "*/test/Test.java" | \
	xargs javac -cp $(CLASSPATH) -d bin

run:
	@if [ ! -f bin/Main.class ]; then make compile; fi
	java -cp $(CLASSPATH) Main

compile_run: compile run

prod: run

clean:
	rm -rf bin

build-dev: clean compile_run

## ────────────────
# Seed
## ────────────────

compile-seed:
	mkdir -p bin
	javac -cp $(CLASSPATH) -d bin src/utils/PasswordUtil.java src/db/Seed.java

seed:
	@if [ ! -f bin/db/Seed.class ]; then make compile-seed; fi
	java -cp $(CLASSPATH) db.Seed

seed-dev: clean compile-seed seed

## ────────────────
# Test
## ────────────────

compile-test:
	mkdir -p bin
	javac -cp $(CLASSPATH) -d bin test/Test.java

tests:
	@if [ ! -f bin/Tests/Test.class ]; then make compile-test; fi
	java -cp $(CLASSPATH) Tests.Test

test-dev: clean compile-test tests

## ────────────────
# DB
## ────────────────

db-create:
	sudo -u postgres psql -c "CREATE DATABASE $(DB_NAME);"

db-migrate:
	sudo -u postgres psql -d $(DB_NAME) -f src/db/schema.sql

db-drop:
	sudo -u postgres psql -c "DROP DATABASE IF EXISTS $(DB_NAME);"

db-seed: seed

db-reset: db-drop db-create db-migrate db-seed
