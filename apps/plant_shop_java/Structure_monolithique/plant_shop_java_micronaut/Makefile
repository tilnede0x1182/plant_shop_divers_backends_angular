
## ────────────────
# Dépendances
## ────────────────

DB_NAME=plant_shop_java_micronaut

compile-install-libs:
	mkdir -p bin
	javac -proc:full -d bin config/InstallCoursier.java

install: compile-install-libs
	java -cp bin config.InstallCoursier

install:

## ────────────────
# Server
## ────────────────

# Variables de base
CLASSPATH = bin:lib/*

compile:
	mkdir -p bin
	javac -proc:full -J-Dorg.jboss.logging.provider=jul -d bin -cp "$(CLASSPATH)" \
		src/utils/*.java \
		src/models/*.java \
		src/repositories/*.java \
		src/security/*.java \
		src/controllers/*.java \
		src/Main.java
run:
	java -cp "$(CLASSPATH)" Main

compile_run: compile run

clean:
	rm -rf bin/*

build-dev: clean compile_run

## ────────────────
# Seed
## ────────────────

compile-seed:
	mkdir -p bin
	javac -proc:full -cp $(CLASSPATH) -d bin src/utils/PasswordUtil.java src/db/Seed.java

seed:
	@if [ ! -f bin/db/Seed.class ]; then make compile-seed; fi
	java -cp $(CLASSPATH) db.Seed

seed-dev: clean compile-seed seed

## ────────────────
# Test
## ────────────────

compile-test:
	mkdir -p bin
	javac -proc:full -cp $(CLASSPATH) -d bin test/Test.java

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
