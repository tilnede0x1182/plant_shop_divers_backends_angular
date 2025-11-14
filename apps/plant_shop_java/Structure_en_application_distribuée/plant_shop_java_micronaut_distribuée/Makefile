ROOT := $(CURDIR)
LIB_DIR := $(ROOT)/lib
SHARED_DIR := $(ROOT)/shared
UTILS_DIR := $(ROOT)/utils
AUTH_DIR := $(ROOT)/auth-service
CATALOG_DIR := $(ROOT)/catalog-service
ORDER_DIR := $(ROOT)/order-service
USER_DIR := $(ROOT)/user-service
GATEWAY_DIR := $(ROOT)/gateway
DB_DIR := $(ROOT)/db
TEST_DIR := $(ROOT)/test

LIB_CP := $(LIB_DIR)/*
SHARED_CP := $(LIB_DIR)/*
SHARED_SRC := $(wildcard $(SHARED_DIR)/model/*.java) $(wildcard $(SHARED_DIR)/utils/*.java) $(wildcard $(SHARED_DIR)/repository/*.java)
UTILS_CP := $(LIB_DIR)/*:$(SHARED_DIR)/bin
UTILS_SRC := $(wildcard $(UTILS_DIR)/src/*.java)
SERVICE_CP := $(LIB_CP):$(SHARED_DIR)/bin:$(UTILS_DIR)/bin

DB_NAME=plant_shop_java_micronaut_distribuee
CLASSPATH := bin:$(shell find lib -name "*.jar" 2>/dev/null | tr '\n' ':')

## ────────────────
# Dépendances
## ────────────────

compile-install-libs:
	mkdir -p bin
	javac -d bin config/InstallCoursier.java

install: compile-install-libs
	java -cp bin config.InstallCoursier

clean:
	rm -rf \
		$(SHARED_DIR)/bin \
		$(UTILS_DIR)/bin \
		$(AUTH_DIR)/bin \
		$(CATALOG_DIR)/bin \
		$(ORDER_DIR)/bin \
		$(USER_DIR)/bin \
		$(GATEWAY_DIR)/bin \
		$(DB_DIR)/bin \
		$(TEST_DIR)/bin \
		$(ROOT)/bin \
		$(ROOT)/build

## ────────────────
# Serveur - Développement
## ────────────────

compile-shared:
	@echo "📦 Compilation shared models & utils"
	@mkdir -p $(SHARED_DIR)/bin
	@javac -proc:none -cp "$(SHARED_CP)" -d $(SHARED_DIR)/bin $(SHARED_SRC)

compile-utils: compile-shared
	@echo "🔧 Compilation utilitaires"
	@mkdir -p $(UTILS_DIR)/bin
	@javac -proc:none -J-Dorg.jboss.logging.provider=jul -cp "$(UTILS_CP)" -d $(UTILS_DIR)/bin $(UTILS_SRC)

compile-auth: compile-utils
	@echo "🔐 Compilation AuthService"
	@mkdir -p $(AUTH_DIR)/bin
	@javac -proc:none -J-Dorg.jboss.logging.provider=jul -cp "$(SERVICE_CP)" -d $(AUTH_DIR)/bin \
		$(wildcard $(AUTH_DIR)/src/controllers/*.java) \
		$(wildcard $(AUTH_DIR)/src/models/*.java) \
		$(wildcard $(AUTH_DIR)/src/repositories/*.java) \
		$(wildcard $(AUTH_DIR)/src/security/*.java)

compile-catalog: compile-utils
	@echo "🌱 Compilation CatalogService"
	@mkdir -p $(CATALOG_DIR)/bin
	@javac -proc:none -J-Dorg.jboss.logging.provider=jul -cp "$(SERVICE_CP)" -d $(CATALOG_DIR)/bin \
		$(wildcard $(CATALOG_DIR)/src/controllers/*.java) \
		$(wildcard $(CATALOG_DIR)/src/models/*.java) \
		$(wildcard $(CATALOG_DIR)/src/repositories/*.java)

compile-order: compile-utils
	@echo "🧾 Compilation OrderService"
	@mkdir -p $(ORDER_DIR)/bin
	@javac -proc:none -J-Dorg.jboss.logging.provider=jul -cp "$(SERVICE_CP)" -d $(ORDER_DIR)/bin \
		$(wildcard $(ORDER_DIR)/src/controllers/*.java) \
		$(wildcard $(ORDER_DIR)/src/models/*.java) \
		$(wildcard $(ORDER_DIR)/src/repositories/*.java)

compile-user: compile-utils
	@echo "👥 Compilation UserService"
	@mkdir -p $(USER_DIR)/bin
	@javac -proc:none -J-Dorg.jboss.logging.provider=jul -cp "$(SERVICE_CP)" -d $(USER_DIR)/bin \
		$(wildcard $(USER_DIR)/src/controllers/*.java) \
		$(wildcard $(USER_DIR)/src/models/*.java) \
		$(wildcard $(USER_DIR)/src/repositories/*.java)

compile-gateway: compile-utils
	@echo "🚪 Compilation Gateway"
	@mkdir -p $(GATEWAY_DIR)/bin
	@javac -proc:none -J-Dorg.jboss.logging.provider=jul -cp "$(SERVICE_CP)" -d $(GATEWAY_DIR)/bin \
		$(wildcard $(GATEWAY_DIR)/src/*.java)

compile: compile-auth compile-catalog compile-order compile-user compile-gateway

## ────────────────
# Run services
## ────────────────

run-auth: compile-auth
	java -cp "$(AUTH_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" AuthService

run-catalog: compile-catalog
	java -cp "$(CATALOG_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" CatalogService

run-order: compile-order
	java -cp "$(ORDER_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" OrderService

run-user: compile-user
	java -cp "$(USER_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" UserService

run-gateway: compile-gateway
	java -cp "$(GATEWAY_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" Gateway

## ────────────────
# PM2 Management
## ────────────────

pm2-start-all:
	java -cp "$(UTILS_DIR)/bin:$(LIB_CP)" util.Pm2Manager start-all

pm2-stop-all:
	java -cp "$(UTILS_DIR)/bin:$(LIB_CP)" util.Pm2Manager stop-all

pm2-start:
ifndef SERVICE
	$(error Usage: make pm2-start SERVICE=<nom>)
endif
	java -cp "$(UTILS_DIR)/bin:$(LIB_CP)" util.Pm2Manager start $(SERVICE)

pm2-stop:
ifndef SERVICE
	$(error Usage: make pm2-stop SERVICE=<nom>)
endif
	java -cp "$(UTILS_DIR)/bin:$(LIB_CP)" util.Pm2Manager stop $(SERVICE)

run: pm2-start-all

compile_run: compile run

build-dev: clean compile

## ────────────────
# Seed
## ────────────────

compile-seed:
	mkdir -p bin
	javac -proc:none -cp $(CLASSPATH) -d bin utils/src/PasswordUtil.java db/Seed.java

seed:
	@if [ ! -f bin/db/Seed.class ]; then make compile-seed; fi
	java -cp $(CLASSPATH) db.Seed

seed-dev: clean compile-seed seed

## ────────────────
# Test
## ────────────────

compile-test:
	mkdir -p bin
	javac -proc:none -cp $(CLASSPATH) -d bin test/Test.java

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
	sudo -u postgres psql -d $(DB_NAME) -f db/schema.sql

db-drop:
	sudo -u postgres psql -c "DROP DATABASE IF EXISTS $(DB_NAME);"

db-seed: seed

db-reset: db-drop db-create db-migrate db-seed
