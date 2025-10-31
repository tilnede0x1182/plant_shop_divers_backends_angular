
## ────────────────
# Dépendances
## ────────────────

DB_NAME=plant_shop_java_micronaut

compile-install-libs:
	mkdir -p bin
	javac -d bin config/InstallLibs.java

install: compile-install-libs
	java -cp bin config/InstallLibs

## ────────────────
# Server
## ────────────────

# Variables de base
CLASSPATH = bin:lib/*

compile:
	mkdir -p bin
	javac -proc:none -J-Dorg.jboss.logging.provider=jul -d bin -cp "$(CLASSPATH)" \
		src/utils/*.java \
		src/models/*.java \
		src/repositories/*.java \
		src/security/*.java \
		src/controllers/*.java \
		src/Application.java

run:
	java -cp "$(CLASSPATH)" Application

compile_run: compile run

clean:
	rm -rf bin/*

build-dev: clean compile_run

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
