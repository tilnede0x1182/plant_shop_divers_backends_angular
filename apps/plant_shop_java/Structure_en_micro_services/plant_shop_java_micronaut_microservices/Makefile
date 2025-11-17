ROOT := $(CURDIR)
LIB_DIR := $(ROOT)/lib
UTILS_DIR := $(ROOT)/utils
AUTH_DIR := $(ROOT)/auth-service
CATALOG_DIR := $(ROOT)/catalog-service
ORDER_DIR := $(ROOT)/order-service
USER_DIR := $(ROOT)/user-service
GATEWAY_DIR := $(ROOT)/gateway
TEST_DIR := $(ROOT)/test
LIB_CP := $(LIB_DIR)/*
UTILS_CP := $(LIB_DIR)/*
UTILS_SRC := $(filter-out $(UTILS_DIR)/src/ApiMapper.java $(UTILS_DIR)/src/Request.java, $(wildcard $(UTILS_DIR)/src/*.java))
SERVICE_CP := $(LIB_CP):$(UTILS_DIR)/bin
DB_NAME=plant_shop_java_micronaut_microservices
CLASSPATH := bin:$(shell find lib -name "*.jar" 2>/dev/null | tr '\n' ':')
## ────────────────
# Dépendances
## ────────────────
compile-install-libs:
	@mkdir -p bin
	@javac -proc:full -d bin config/InstallCoursier.java
install: compile-install-libs
	@java -cp bin config.InstallCoursier
clean:
	@rm -rf \
		$(UTILS_DIR)/bin \
		$(AUTH_DIR)/bin \
		$(CATALOG_DIR)/bin \
		$(ORDER_DIR)/bin \
		$(USER_DIR)/bin \
		$(GATEWAY_DIR)/bin \
		$(TEST_DIR)/bin \
		$(ROOT)/bin \
		$(ROOT)/build
## ────────────────
# Serveur - Développement
## ────────────────
compile-utils:
	@echo "🔧 Compilation utilitaires"
	@mkdir -p $(UTILS_DIR)/bin
	@javac -proc:full -J-Dorg.jboss.logging.provider=jul -cp "$(UTILS_CP)" -d $(UTILS_DIR)/bin $(UTILS_SRC)
compile-auth: compile-utils
	@echo "🔐 Compilation AuthService"
	@mkdir -p $(AUTH_DIR)/bin
	@javac -proc:full -cp "$(SERVICE_CP)" -d $(AUTH_DIR)/bin \
		$(wildcard $(AUTH_DIR)/src/*.java) \
		$(wildcard $(AUTH_DIR)/src/util/*.java) \
		$(wildcard $(AUTH_DIR)/src/controllers/*.java) \
		$(wildcard $(AUTH_DIR)/src/model/*.java) \
		$(wildcard $(AUTH_DIR)/src/repository/*.java) \
		$(wildcard $(AUTH_DIR)/src/security/*.java)
compile-catalog: compile-utils
	@echo "🌱 Compilation CatalogService"
	@mkdir -p $(CATALOG_DIR)/bin
	@javac -proc:full -cp "$(SERVICE_CP)" -d $(CATALOG_DIR)/bin \
		$(wildcard $(CATALOG_DIR)/src/*.java) \
		$(wildcard $(CATALOG_DIR)/src/util/*.java) \
		$(wildcard $(CATALOG_DIR)/src/security/*.java) \
		$(wildcard $(CATALOG_DIR)/src/controllers/*.java) \
		$(wildcard $(CATALOG_DIR)/src/model/*.java) \
		$(wildcard $(CATALOG_DIR)/src/repository/*.java)
compile-order: compile-utils
	@echo "🧾 Compilation OrderService"
	@mkdir -p $(ORDER_DIR)/bin
	@javac -proc:full -cp "$(SERVICE_CP)" -d $(ORDER_DIR)/bin \
		$(wildcard $(ORDER_DIR)/src/*.java) \
		$(wildcard $(ORDER_DIR)/src/util/*.java) \
		$(wildcard $(ORDER_DIR)/src/security/*.java) \
		$(wildcard $(ORDER_DIR)/src/controllers/*.java) \
		$(wildcard $(ORDER_DIR)/src/model/*.java) \
		$(wildcard $(ORDER_DIR)/src/repository/*.java)
compile-user: compile-utils
	@echo "👥 Compilation UserService"
	@mkdir -p $(USER_DIR)/bin
	@javac -proc:full -cp "$(SERVICE_CP)" -d $(USER_DIR)/bin \
		$(wildcard $(USER_DIR)/src/*.java) \
		$(wildcard $(USER_DIR)/src/util/*.java) \
		$(wildcard $(USER_DIR)/src/security/*.java) \
		$(wildcard $(USER_DIR)/src/controllers/*.java) \
		$(wildcard $(USER_DIR)/src/model/*.java) \
		$(wildcard $(USER_DIR)/src/repository/*.java)
compile-gateway: compile-utils
	@echo "🚪 Compilation Gateway"
	@mkdir -p $(GATEWAY_DIR)/bin
	@javac -proc:full -cp "$(SERVICE_CP)" -d $(GATEWAY_DIR)/bin \
		$(wildcard $(GATEWAY_DIR)/src/*.java)
compile: compile-auth compile-catalog compile-order compile-user compile-gateway
## ────────────────
# Run services
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

run-with-logs: pm2-stop-all-safe
	@TS=$$(date +"%Y%m%d_%H%M%S"); \
	BASE="$(LOG_DIR)"; \
	DIR="$$BASE/$$TS"; \
	mkdir -p "$$DIR"; \
	echo "🗂️  Logs dans $$DIR"; \
	nohup java -cp "$(AUTH_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" AuthService > "$$DIR/auth-service.log" 2>&1 & \
	echo "   ↳ AuthService → $$DIR/auth-service.log"; \
	nohup java -cp "$(CATALOG_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" CatalogService > "$$DIR/catalog-service.log" 2>&1 & \
	echo "   ↳ CatalogService → $$DIR/catalog-service.log"; \
	nohup java -cp "$(ORDER_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" OrderService > "$$DIR/order-service.log" 2>&1 & \
	echo "   ↳ OrderService → $$DIR/order-service.log"; \
	nohup java -cp "$(USER_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" UserService > "$$DIR/user-service.log" 2>&1 & \
	echo "   ↳ UserService → $$DIR/user-service.log"; \
	nohup java -cp "$(GATEWAY_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" Gateway > "$$DIR/gateway.log" 2>&1 & \
	echo "   ↳ Gateway → $$DIR/gateway.log"; \
	echo "🚀 Services démarrés (dossier $$DIR)"

compile-run-with-logs: compile run-with-logs

## ────────────────
# PM2 Management
## ────────────────
pm2-start-all: pm2-stop-all-safe
	@java -cp "$(UTILS_DIR)/bin:$(LIB_CP)" util.Pm2Manager start-all
pm2-stop-all-safe:
	@java -cp "$(UTILS_DIR)/bin:$(LIB_CP)" util.Pm2Manager stop-all-safe
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
build-dev: clean compile
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
# DB (MICROSERVICES)
## ────────────────
db-create:
	@sudo -u postgres psql -c "CREATE DATABASE $(DB_NAME);"
db-drop:
	@sudo -u postgres psql -c "DROP DATABASE IF EXISTS $(DB_NAME);"
db-migrate-catalog:
	@echo "🐘 Migration catalog-service..."
	@sudo -u postgres psql -d $(DB_NAME) -f catalog-service/db/schema.sql
db-migrate-order:
	@echo "🐘 Migration order-service..."
	@sudo -u postgres psql -d $(DB_NAME) -f order-service/db/schema.sql
db-migrate-user:
	@echo "🐘 Migration user-service..."
	@sudo -u postgres psql -d $(DB_NAME) -f user-service/db/schema.sql
db-migrate: db-migrate-user db-migrate-catalog db-migrate-order
	@echo "✅ Toutes les migrations sont terminées."
compile-seed-catalog: compile-utils
	@echo "🔧 Compilation Seed catalog-service"
	@mkdir -p $(CATALOG_DIR)/bin
	@javac -proc:full -cp "$(SERVICE_CP)" -d $(CATALOG_DIR)/bin catalog-service/db/Seed.java -proc:none
compile-seed-order: compile-utils
	@echo "🔧 Compilation Seed order-service"
	@mkdir -p $(ORDER_DIR)/bin
	@javac -proc:full -cp "$(SERVICE_CP)" -d $(ORDER_DIR)/bin order-service/db/Seed.java -proc:none
compile-seed-user: compile-utils
	@echo "🔧 Compilation Seed user-service"
	@mkdir -p $(USER_DIR)/bin
	@javac -proc:full -cp "$(SERVICE_CP)" -d $(USER_DIR)/bin user-service/db/Seed.java -proc:none
seed-catalog: compile-seed-catalog
	@echo "🌱 Seed catalog-service..."
	@java -cp "$(CATALOG_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" db.Seed
seed-order: compile-seed-order
	@echo "🌱 Seed order-service..."
	@java -cp "$(ORDER_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" db.Seed
seed-user: compile-seed-user
	@echo "🌱 Seed user-service..."
	@java -cp "$(USER_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" db.Seed
seed: seed-user seed-catalog seed-order
	@echo "✅ Seed global terminé."
seed-dev: clean seed
db-reset: db-drop db-create db-migrate seed
