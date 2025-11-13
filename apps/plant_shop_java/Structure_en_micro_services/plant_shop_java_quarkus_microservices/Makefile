
## ────────────────
# Dépendances
## ────────────────

DB_NAME=plant_shop_java_quarkus

compile-install-libs:
	mkdir -p bin
	javac -d bin config/InstallCoursier.java

install: compile-install-libs
	java -cp bin config.InstallCoursier

clean:
	rm -rf bin/*

## ────────────────
# Serveur - Développement
## ────────────────

# Variables de base
CLASSPATH = bin:lib/*

compile:
	mkdir -p bin
	javac -J-Dorg.jboss.logging.provider=jul -d bin -cp "$(CLASSPATH)" \
		src/utils/*.java \
		src/models/*.java \
		src/repositories/*.java \
		src/security/*.java \
		src/controllers/*.java \
		src/Main.java
run:
	java -cp "$(CLASSPATH)" Main

compile_run: compile run

build-dev: clean compile_run

## ────────────────
# # Serveur - Production
## ────────────────

SRCS := $(shell find src -type f -name '*.java')

build-prod: clean
	mkdir -p bin build
	@echo "Compiling $$(echo $(SRCS) | wc -w) sources..."
	javac -J-Dorg.jboss.logging.provider=jul -d bin -cp "lib/*" $(SRCS)
	jar --create --file build/app-prod.jar --main-class=Main -C bin .

run-prod:
	# JVM optimisée pour prod (démarrage plus rapide, heap fixe, GC G1)
	java \
	  -XX:+TieredCompilation \
	  -XX:TieredStopAtLevel=1 \
	  -Dmicronaut.env=prod \
	  -Djava.security.egd=file:/dev/./urandom \
	  -Xms512m -Xmx1g \
	  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
	  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=build/heap.hprof \
	  -cp "bin:lib/*" Main

prod: build-prod run-prod

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
