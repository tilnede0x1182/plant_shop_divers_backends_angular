ROOT := $(CURDIR)
LIB_DIR := $(ROOT)/lib
UTILS_DIR := $(ROOT)/utils
AUTH_DIR := $(ROOT)/auth-service
CATALOG_DIR := $(ROOT)/catalog-service
ORDER_DIR := $(ROOT)/order-service
USER_DIR := $(ROOT)/user-service
GATEWAY_DIR := $(ROOT)/gateway
TEST_DIR := $(ROOT)/Tests

LIB_CP := $(LIB_DIR)/*
UTILS_CP := $(LIB_DIR)/*
UTILS_SRC := $(wildcard $(UTILS_DIR)/*.java)
SERVICE_CP := $(LIB_CP):$(UTILS_DIR)/bin

DB_NAME=plant_shop_java_microservices
CLASSPATH := bin:$(shell find lib -name "*.jar" | tr '\n' ':')

## ────────────────
# Dépendances - compilation
## ────────────────

install:
	@echo "📦 Installation des dépendances Java"
	@mkdir -p $(ROOT)/bin
	@javac -d $(ROOT)/bin $(ROOT)/config/InstallLibs.java
	@java -cp $(ROOT)/bin config.InstallLibs

## ────────────────
# Server
## ────────────────

compile-utils:
	@echo "🔧 Compilation utilitaires"
	@mkdir -p $(UTILS_DIR)/bin
	@javac -cp "$(UTILS_CP)" -d $(UTILS_DIR)/bin $(UTILS_SRC)

compile-auth: compile-utils
	@echo "🔐 Compilation AuthService"
	@mkdir -p $(AUTH_DIR)/bin
	@javac -cp "$(SERVICE_CP)" -d $(AUTH_DIR)/bin $(AUTH_DIR)/AuthService.java

compile-catalog: compile-utils
	@echo "🌱 Compilation CatalogService"
	@mkdir -p $(CATALOG_DIR)/bin
	@javac -cp "$(SERVICE_CP)" -d $(CATALOG_DIR)/bin \
		$(wildcard $(CATALOG_DIR)/src/controller/*.java) \
		$(wildcard $(CATALOG_DIR)/src/model/*.java) \
		$(wildcard $(CATALOG_DIR)/src/repository/*.java) \
		$(CATALOG_DIR)/CatalogService.java

compile-order: compile-utils
	@echo "🧾 Compilation OrderService"
	@mkdir -p $(ORDER_DIR)/bin
	@javac -cp "$(SERVICE_CP)" -d $(ORDER_DIR)/bin \
		$(wildcard $(ORDER_DIR)/src/controller/*.java) \
		$(wildcard $(ORDER_DIR)/src/model/*.java) \
		$(wildcard $(ORDER_DIR)/src/repository/*.java) \
		$(ORDER_DIR)/OrderService.java

compile-user: compile-utils
	@echo "👥 Compilation UserService"
	@mkdir -p $(USER_DIR)/bin
	@javac -cp "$(SERVICE_CP)" -d $(USER_DIR)/bin \
		$(wildcard $(USER_DIR)/src/controller/*.java) \
		$(wildcard $(USER_DIR)/src/model/*.java) \
		$(wildcard $(USER_DIR)/src/repository/*.java) \
		$(USER_DIR)/UserService.java

compile-gateway: compile-utils
	@echo "🚪 Compilation Gateway"
	@mkdir -p $(GATEWAY_DIR)/bin
	@javac -cp "$(SERVICE_CP)" -d $(GATEWAY_DIR)/bin \
		$(wildcard $(GATEWAY_DIR)/src/*.java) \
		$(GATEWAY_DIR)/Gateway.java

compile: compile-auth compile-catalog compile-order compile-user compile-gateway

## ────────────────
# Dépendances - run
## ────────────────

run-auth: compile-auth
	@java -cp "$(AUTH_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" AuthService

run-catalog: compile-catalog
	@java -cp "$(CATALOG_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" CatalogService

run-order: compile-order
	@java -cp "$(ORDER_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" OrderService

run-user: compile-user
	@java -cp "$(USER_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" UserService

run-gateway: compile-gateway
	@java -cp "$(GATEWAY_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" Gateway

## ────────────────
# Dépendances - pm2
## ────────────────

pm2-start-all:
	@java -cp "$(UTILS_DIR)/bin:$(LIB_CP)" util.Pm2Manager start-all

pm2-stop-all:
	@java -cp "$(UTILS_DIR)/bin:$(LIB_CP)" util.Pm2Manager stop-all

pm2-start:
ifndef SERVICE
	@$(error Usage: make pm2-start SERVICE=<nom>)
endif
	@java -cp "$(UTILS_DIR)/bin:$(LIB_CP)" util.Pm2Manager start $(SERVICE)

pm2-stop:
ifndef SERVICE
	@$(error Usage: make pm2-stop SERVICE=<nom>)
endif
	@java -cp "$(UTILS_DIR)/bin:$(LIB_CP)" util.Pm2Manager stop $(SERVICE)

run: pm2-start-all

compile_run: compile run

clean:
	@rm -rf \
		$(UTILS_DIR)/bin \
		$(AUTH_DIR)/bin \
		$(CATALOG_DIR)/bin \
		$(ORDER_DIR)/bin \
		$(USER_DIR)/bin \
		$(GATEWAY_DIR)/bin \
		$(TEST_DIR)/bin \
		$(ROOT)/bin

build-dev: clean compile

## ────────────────
# Test
## ────────────────

compile-test:
	@mkdir -p bin
	@javac -cp $(CLASSPATH) -d bin test/Test.java

tests:
	@if [ ! -f bin/Tests/Test.class ]; then make compile-test; fi
	@java -cp $(CLASSPATH) Tests.Test

test-dev: clean compile-test tests


## ────────────────
# DB (CIBLES MODIFIÉES)
## ────────────────

db-create:
	@sudo -u postgres psql -c "CREATE DATABASE $(DB_NAME);"

db-drop:
	@sudo -u postgres psql -c "DROP DATABASE IF EXISTS $(DB_NAME);"

# --- Cibles de migration par service ---
db-migrate-users:
	@echo "🐘 Migration user-service..."
	@sudo -u postgres psql -d $(DB_NAME) -f user-service/db/schema.sql

db-migrate-catalog:
	@echo "🐘 Migration catalog-service..."
	@sudo -u postgres psql -d $(DB_NAME) -f catalog-service/db/schema.sql

db-migrate-orders:
	@echo "🐘 Migration order-service..."
	@sudo -u postgres psql -d $(DB_NAME) -f order-service/db/schema.sql

# --- Cible de migration globale ---
# Note: L'ordre est important à cause des clés étrangères
db-migrate: db-migrate-users db-migrate-catalog db-migrate-orders
	@echo "✅ Toutes les migrations sont terminées."

# --- Cibles de compilation des Seeders ---
compile-seed-users: compile-utils
	@echo "🔧 Compilation Seed user-service"
	@mkdir -p $(USER_DIR)/bin
	@javac -cp "$(SERVICE_CP)" -d $(USER_DIR)/bin user-service/db/Seed.java -proc:none

compile-seed-catalog: compile-utils
	@echo "🔧 Compilation Seed catalog-service"
	@mkdir -p $(CATALOG_DIR)/bin
	@javac -cp "$(SERVICE_CP)" -d $(CATALOG_DIR)/bin catalog-service/db/Seed.java -proc:none

compile-seed-orders: compile-utils
	@echo "🔧 Compilation Seed order-service"
	@mkdir -p $(ORDER_DIR)/bin
	@javac -cp "$(SERVICE_CP)" -d $(ORDER_DIR)/bin order-service/db/Seed.java -proc:none

# --- Cibles de Seed par service ---
seed-users: compile-seed-users
	@echo "🌱 Seed user-service..."
	@java -cp "$(USER_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" db.Seed

seed-catalog: compile-seed-catalog
	@echo "🌱 Seed catalog-service..."
	@java -cp "$(CATALOG_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" db.Seed

seed-orders: compile-seed-orders
	@echo "🌱 Seed order-service..."
	@java -cp "$(ORDER_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" db.Seed

# --- Cible de Seed globale ---
# Note: L'ordre est important (les commandes dépendent des utilisateurs et des plantes)
seed: seed-users seed-catalog seed-orders
	@echo "✅ Seed global terminé."

# --- Cible de Reset globale ---
db-reset: db-drop db-create db-migrate seed
