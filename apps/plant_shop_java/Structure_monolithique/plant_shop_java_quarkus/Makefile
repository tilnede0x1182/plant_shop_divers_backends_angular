
## ────────────────
# Dépendances
## ────────────────

DB_NAME=plant_shop_java_quarkus

compile-install-libs:
	@mkdir -p bin
	@javac -proc:full -d bin config/InstallCoursier.java src/utils/QuarkusBundleBuilder.java

install: compile-install-libs
	@java -cp bin config.InstallCoursier
	@java -cp bin utils.QuarkusBundleBuilder

clean:
	@rm -rf bin/*

## ────────────────
# Serveur - Développement
## ────────────────

# Variables de base
CLASSPATH = bin:lib/*

compile:
	@mkdir -p bin/META-INF
	@cp src/META-INF/beans.xml bin/META-INF/
	@javac -proc:full -J-Dorg.jboss.logging.provider=jul -d bin -cp "$(CLASSPATH)" \
		src/utils/*.java \
		src/models/*.java \
		src/repositories/*.java \
		src/security/*.java \
		src/controllers/*.java \
		src/Main.java
	@if [ -d src/resources ]; then cp -R src/resources/. bin/; fi
	@java -cp "$(CLASSPATH)" utils.QuarkusBundleBuilder

run:
	@if [ ! -f .quarkus/quarkus-app/quarkus-run.jar ]; then $(MAKE) compile; fi
	@java -Djava.util.logging.manager=org.jboss.logmanager.LogManager -jar .quarkus/quarkus-app/quarkus-run.jar

compile_run: compile run

build-dev: clean compile
## ────────────────
# # Serveur - Production
## ────────────────

SRCS := $(shell find src -type f -name '*.java')

build-prod: clean
			@mkdir -p bin build
			@javac -proc:full -J-Dorg.jboss.logging.provider=jul -d bin -cp "lib/*" $(SRCS)
			@java -cp "bin:lib/*" utils.QuarkusBundleBuilder
			@jar --create --file build/app-prod.jar --main-class=Main -C bin .

prod:
			@if [ ! -f build/app-prod.jar ] || [ ! -f .quarkus/quarkus-app/quarkus-run.jar ]; then $(MAKE) build-prod; fi
			@java \
							-XX:+TieredCompilation \
							-XX:TieredStopAtLevel=1 \
							-Dmicronaut.env=prod \
							-Djava.security.egd=file:/dev/./urandom \
							-Xms512m -Xmx1g \
							-XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
							-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=build/heap.hprof \
							-jar build/app-prod.jar

## ────────────────
# Seed
## ────────────────

compile-seed:
	@mkdir -p bin
	@javac -proc:full -cp $(CLASSPATH) -d bin src/utils/PasswordUtil.java src/db/Seed.java

seed:
	@if [ ! -f bin/db/Seed.class ]; then make compile-seed; fi
	@java -cp $(CLASSPATH) db.Seed

seed-dev: clean compile-seed seed

## ────────────────
# Test
## ────────────────

compile-test:
	@mkdir -p bin
	@javac -proc:full -cp $(CLASSPATH) -d bin test/Test.java

tests:
	@if [ ! -f bin/Tests/Test.class ]; then make compile-test; fi
	@java -cp $(CLASSPATH) Tests.Test

test-dev: clean compile-test tests

## ────────────────
# DB
## ────────────────

db-create:
	@sudo -u postgres psql -c "CREATE DATABASE $(DB_NAME);"

db-migrate:
	@sudo -u postgres psql -d $(DB_NAME) -f src/db/schema.sql

db-drop:
	@sudo -u postgres psql -c "DROP DATABASE IF EXISTS $(DB_NAME);"

db-seed: seed

db-reset: db-drop db-create db-migrate db-seed
