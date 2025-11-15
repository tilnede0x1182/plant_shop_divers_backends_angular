CLASSPATH=bin:lib/postgresql-42.7.8.jar
CLASSPATH:=\$(CLASSPATH):lib/json-20210307.jar
CLASSPATH:=$(CLASSPATH):lib/jbcrypt-0.4.jar

## ────────────────
# Server
## ────────────────

compile:
	mkdir -p bin
	find src -type f -name "*.java" ! -name "Seed.java" ! -name "Test.java" | \
	xargs javac -proc:full -cp $(CLASSPATH) -d bin

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
	javac -proc:full -cp $(CLASSPATH) -d bin src/utils/PasswordUtil.java src/db/Seed.java

seed:
	@if [ ! -f bin/db/Seed.class ]; then make compile-seed; fi
	java -cp $(CLASSPATH) db.Seed

seed-dev: clean compile-seed seed

## ────────────────
# DB
## ────────────────

db-create:
	sudo -u postgres psql -c "CREATE DATABASE plant_shop_java;"

db-migrate:
	sudo -u postgres psql -d plant_shop_java -f src/db/schema.sql

db-drop:
	sudo -u postgres psql -c "DROP DATABASE plant_shop_java;"

db-seed: seed

db-reset: db-drop db-create db-migrate db-seed

## ────────────────
# Test
## ────────────────

compile-test:
	mkdir -p bin
	javac -proc:full -cp $(CLASSPATH) -d bin Tests/Test.java

tests:
	@if [ ! -f bin/Tests/Test.class ]; then make compile-test; fi
	java -cp $(CLASSPATH) Tests.Test

seed-dev: clean compile-seed seed

build-dev: clean compile_run
