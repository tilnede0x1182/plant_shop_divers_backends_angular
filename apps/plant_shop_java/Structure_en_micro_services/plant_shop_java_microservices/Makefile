ROOT := $(CURDIR)
LIB_DIR := $(ROOT)/lib
UTILS_DIR := $(ROOT)/utils
AUTH_DIR := $(ROOT)/auth-service
CATALOG_DIR := $(ROOT)/catalog-service
ORDER_DIR := $(ROOT)/order-service
USER_DIR := $(ROOT)/user-service
GATEWAY_DIR := $(ROOT)/gateway
DB_DIR := $(ROOT)/db
TEST_DIR := $(ROOT)/Tests

LIB_CP := $(LIB_DIR)/*
UTILS_CP := $(LIB_DIR)/*
UTILS_SRC := $(wildcard $(UTILS_DIR)/*.java)
SERVICE_CP := $(LIB_CP):$(UTILS_DIR)/bin

.PHONY: install compile compile-auth compile-catalog compile-order compile-user compile-gateway compile-utils \
        run-auth run-catalog run-order run-user run-gateway \
        pm2-start-all pm2-stop-all pm2-start pm2-stop seed tests clean

install:
    @echo "📦 Installation des dépendances Java"
    @mkdir -p $(ROOT)/bin
    @javac -d $(ROOT)/bin $(ROOT)/config/InstallLibs.java
    @java -cp $(ROOT)/bin config.InstallLibs

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
    @javac -cp "$(SERVICE_CP)" -d $(CATALOG_DIR)/bin $(CATALOG_DIR)/CatalogService.java

compile-order: compile-utils
    @echo "🧾 Compilation OrderService"
    @mkdir -p $(ORDER_DIR)/bin
    @javac -cp "$(SERVICE_CP)" -d $(ORDER_DIR)/bin $(ORDER_DIR)/OrderService.java

compile-user: compile-utils
    @echo "👥 Compilation UserService"
    @mkdir -p $(USER_DIR)/bin
    @javac -cp "$(SERVICE_CP)" -d $(USER_DIR)/bin $(USER_DIR)/UserService.java

compile-gateway: compile-utils
    @echo "🚪 Compilation Gateway"
    @mkdir -p $(GATEWAY_DIR)/bin
    @javac -cp "$(SERVICE_CP)" -d $(GATEWAY_DIR)/bin $(GATEWAY_DIR)/Gateway.java

compile: compile-auth compile-catalog compile-order compile-user compile-gateway

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

pm2-start-all: compile
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

seed: compile-utils
    @echo "🌱 Seed de la base"
    @mkdir -p $(DB_DIR)/bin
    @javac -cp "$(SERVICE_CP)" -d $(DB_DIR)/bin $(DB_DIR)/Seed.java
    @java -cp "$(DB_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" db.Seed

Tests/Test.class: compile-gateway
    @mkdir -p $(TEST_DIR)/bin
    @javac -cp "$(SERVICE_CP)" -d $(TEST_DIR)/bin $(TEST_DIR)/Test.java

tests: Tests/Test.class
    java -cp "$(TEST_DIR)/bin:$(UTILS_DIR)/bin:$(LIB_CP)" Tests.Test

clean:
    rm -rf \
        $(UTILS_DIR)/bin \
        $(AUTH_DIR)/bin \
        $(CATALOG_DIR)/bin \
        $(ORDER_DIR)/bin \
        $(USER_DIR)/bin \
        $(GATEWAY_DIR)/bin \
        $(DB_DIR)/bin \
        $(TEST_DIR)/bin \
        $(ROOT)/bin
