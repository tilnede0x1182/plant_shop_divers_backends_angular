ROOT := $(CURDIR)
LIB_CP := lib/*
UTILS_CP := lib/*
UTILS_SRC := $(wildcard utils/*.java)

.PHONY: install compile compile-auth compile-catalog compile-order compile-user compile-gateway compile-utils \
        run-auth run-catalog run-order run-user run-gateway \
        pm2-start-all pm2-stop-all pm2-start pm2-stop seed tests clean

install:
	@echo "📦 Installation des dépendances Java"
	@mkdir -p bin
	@javac -d bin config/InstallLibs.java
	@java -cp bin config.InstallLibs

compile-utils:
	@echo "🔧 Compilation utilitaires"
	@mkdir -p utils/bin
	@javac -cp "$(UTILS_CP)" -d utils/bin $(UTILS_SRC)

compile-auth: compile-utils
	@echo "🔐 Compilation AuthService"
	@mkdir -p auth-service/bin
	@javac -cp "$(LIB_CP):utils/bin" -d auth-service/bin auth-service/AuthService.java

compile-catalog: compile-utils
	@echo "🌱 Compilation CatalogService"
	@mkdir -p catalog-service/bin
	@javac -cp "$(LIB_CP):utils/bin" -d catalog-service/bin catalog-service/CatalogService.java

compile-order: compile-utils
	@echo "🧾 Compilation OrderService"
	@mkdir -p order-service/bin
	@javac -cp "$(LIB_CP):utils/bin" -d order-service/bin order-service/OrderService.java

compile-user: compile-utils
	@echo "👥 Compilation UserService"
	@mkdir -p user-service/bin
	@javac -cp "$(LIB_CP):utils/bin" -d user-service/bin user-service/UserService.java

compile-gateway: compile-utils
	@echo "🚪 Compilation Gateway"
	@mkdir -p gateway/bin
	@javac -cp "$(LIB_CP):utils/bin" -d gateway/bin gateway/Gateway.java

compile: compile-auth compile-catalog compile-order compile-user compile-gateway

run-auth: compile-auth
	java -cp "auth-service/bin:utils/bin:$(LIB_CP)" AuthService

run-catalog: compile-catalog
	java -cp "catalog-service/bin:utils/bin:$(LIB_CP)" CatalogService

run-order: compile-order
	java -cp "order-service/bin:utils/bin:$(LIB_CP)" OrderService

run-user: compile-user
	java -cp "user-service/bin:utils/bin:$(LIB_CP)" UserService

run-gateway: compile-gateway
	java -cp "gateway/bin:utils/bin:$(LIB_CP)" Gateway

pm2-start-all: compile
	java -cp "utils/bin:$(LIB_CP)" util.Pm2Manager start-all

pm2-stop-all:
	java -cp "utils/bin:$(LIB_CP)" util.Pm2Manager stop-all

pm2-start:
ifndef SERVICE
	$(error Usage: make pm2-start SERVICE=<nom>)
endif
	java -cp "utils/bin:$(LIB_CP)" util.Pm2Manager start $(SERVICE)

pm2-stop:
ifndef SERVICE
	$(error Usage: make pm2-stop SERVICE=<nom>)
endif
	java -cp "utils/bin:$(LIB_CP)" util.Pm2Manager stop $(SERVICE)

seed: compile-utils
	@echo "🌱 Seed de la base"
	@mkdir -p db/bin
	@javac -cp "$(LIB_CP):utils/bin" -d db/bin db/Seed.java
	@java -cp "db/bin:utils/bin:$(LIB_CP)" db.Seed

Tests/Test.class: compile-gateway
	@mkdir -p Tests/bin
	@javac -cp "$(LIB_CP):utils/bin" -d Tests/bin Tests/Test.java

tests: Tests/Test.class
	java -cp "Tests/bin:utils/bin:$(LIB_CP)" Tests.Test

clean:
	rm -rf utils/bin auth-service/bin catalog-service/bin order-service/bin user-service/bin gateway/bin db/bin Tests/bin bin
