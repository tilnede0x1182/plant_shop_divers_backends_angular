# ======================================================
# 🧩 Backend NestJS
# ======================================================

# Run backend NestJS
run:
	SERVE_SSR=true PORT=4100 node dist/apps/plant_shop_nest/main.js

# Run Nest backend seul (API, sans SSR Angular)
run-dev-back:
	npm run dev-back

# Tests Nest
test-e2e:
	npm run test-e2e

test-routes:
	npm run test-routes

# Build Nest
buid-back:
	npm run build-backend

# ======================================================
# 📜 Backend Manifest ABANDON
# ======================================================

run-proxy-manifest:
	node apps/plant_shop_manifest/proxy.js

# Run Manifest backend (API seule, mode dev)
run-manifest:
	PORT=4120 pnpm run --prefix apps/plant_shop_manifest dev

# Build Manifest backend
build-manifest:
	pnpm run --prefix apps/plant_shop_manifest build

# Start Manifest backend (mode prod)
prod-manifest:
	PORT=4120 pnpm run --prefix apps/plant_shop_manifest start

# Build puis exécution en prod
build-prod: build-manifest prod-manifest

# ======================================================
# 🐹 Backend Go
# ======================================================

run-backend-go:
	go run apps/plant_shop_go/main.go --serve

compile-backend-go:
	go build -o apps/plant_shop_go/bin/server apps/plant_shop_go/main.go

seed-backend-go:
	go run apps/plant_shop_go/cmd/seed.go

compile-run-backend-go: compile-backend-go run-backend-go

prod-backend-go:
	go build -o apps/plant_shop_go/bin/server apps/plant_shop_go/main.go && apps/plant_shop_go/bin/server --serve

# ======================================================
# 🦀 Backends Rust avec SQLx
# ======================================================

run-backend-rust-sqlx:
	make -C apps/plant_shop_rust/plant_shop_rust_sqlx run

compile-backend-rust-sqlx:
	make -C apps/plant_shop_rust/plant_shop_rust_sqlx compile

seed-backend-rust-sqlx:
	make -C apps/plant_shop_rust/plant_shop_rust_sqlx seed

compile-run-backend-rust-sqlx: compile-backend-rust-sqlx run-backend-rust-sqlx

prod-backend-rust-sqlx:
	make -C apps/plant_shop_rust/plant_shop_rust_sqlx prod

# ======================================================
# 🦀 Backends Rust avec SeeORM
# ======================================================

run-backend-rust-see-orm:
	make -C apps/plant_shop_rust/plant_shop_rust_see_orm run

compile-backend-rust-see-orm:
	make -C apps/plant_shop_rust/plant_shop_rust_see_orm compile

seed-backend-rust-see-orm:
	make -C apps/plant_shop_rust/plant_shop_rust_see_orm seed

compile-run-backend-rust-see-orm: compile-backend-rust-see-orm run-backend-rust-see-orm

prod-backend-rust-see-orm:
	make -C apps/plant_shop_rust/plant_shop_rust_see_orm prod

# ======================================================
# 🚀 Backend C++ (Drogon)
# ======================================================

run-backend-cpp:
	make -C apps/plant_shop_cpp run

compile-backend-cpp:
	make -C apps/plant_shop_cpp compile

seed-backend-cpp:
	make -C apps/plant_shop_cpp seed

compile-run-backend-cpp: compile-backend-cpp run-backend-cpp

prod-backend-cpp:
	make -C apps/plant_shop_cpp prod

test-backend-cpp:
	make -C apps/plant_shop_cpp tests

compile-test-backend-cpp:
	make -C apps/plant_shop_cpp compile-test

# ======================================================
# ☕ Backend Java (HTTPServer)
# ======================================================

run-backend-java:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java run

compile-backend-java:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java compile

seed-backend-java:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java seed

compile-run-backend-java: compile-backend-java run-backend-java

prod-backend-java:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java prod

test-backend-java:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java tests

compile-test-backend-java:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java compile-test

# ======================================================
# ☕ Backend Javalin
# ======================================================

run-backend-javalin:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_javalin run

compile-backend-javalin:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_javalin compile

seed-backend-javalin:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_javalin seed

compile-run-backend-javalin: compile-backend-javalin run-backend-javalin

prod-backend-javalin:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_javalin prod

test-backend-javalin:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_javalin tests

compile-test-backend-javalin:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_javalin compile-test

# ======================================================
# ☕ Backend Micronaut
# ======================================================

run-backend-micronaut:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_micronaut run

compile-backend-micronaut:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_micronaut compile

seed-backend-micronaut:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_micronaut seed

compile-run-backend-micronaut: compile-backend-micronaut run-backend-micronaut

prod-backend-micronaut:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_micronaut build-dev

test-backend-micronaut:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_micronaut tests

compile-test-backend-micronaut:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_micronaut compile-test

# ======================================================
# ☕ Backend Java Quarkus
# ======================================================

run-backend-java-quarkus:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_quarkus run

compile-backend-java-quarkus:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_quarkus compile

seed-backend-java-quarkus:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_quarkus seed

compile-run-backend-java-quarkus: compile-backend-java-quarkus run-backend-java-quarkus

prod-backend-java-quarkus:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_quarkus build-dev

test-backend-java-quarkus:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_quarkus tests

compile-test-backend-java-quarkus:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_quarkus compile-test

# ======================================================
# ☕ Backend Java Spring Boot Security
# ======================================================

run-backend-java-spring-boot-security:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security run

compile-backend-java-spring-boot-security:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security compile

seed-backend-java-spring-boot-security:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security seed

compile-run-backend-java-spring-boot-security: compile-backend-java-spring-boot-security run-backend-java-spring-boot-security

prod-backend-java-spring-boot-security:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security build-dev

test-backend-java-spring-boot-security:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security tests

compile-test-backend-java-spring-boot-security:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security compile-test

# ======================================================
# ☕ Backend Java Spring Boot Security (Hibernate)
# ======================================================

run-backend-java-spring-boot-security-hibernate:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate run

compile-backend-java-spring-boot-security-hibernate:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate compile

seed-backend-java-spring-boot-security-hibernate:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate seed

compile-run-backend-java-spring-boot-security-hibernate: compile-backend-java-spring-boot-security-hibernate run-backend-java-spring-boot-security-hibernate

prod-backend-java-spring-boot-security-hibernate:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate build-dev

test-backend-java-spring-boot-security-hibernate:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate tests

compile-test-backend-java-spring-boot-security-hibernate:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate compile-test

# ======================================================
# ☕ Backend Java ActiveWeb
# ======================================================

run-backend-java-activeweb:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_active_web run

compile-backend-java-activeweb:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_active_web compile

seed-backend-java-activeweb:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_active_web seed

compile-run-backend-java-activeweb: compile-backend-java-activeweb run-backend-java-activeweb

prod-backend-java-activeweb:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_active_web build-dev

test-backend-java-activeweb:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_active_web tests

compile-test-backend-java-activeweb:
	make -C apps/plant_shop_java/Structure_monolithique/plant_shop_java_active_web compile-test

# ======================================================
# ☕ Backend Java Architecture distribuée
# ======================================================

run-backend-java-distrib:
	make -C "apps/plant_shop_java/Structure_en_application_distribuée/plant_shop_java_distribuée" run-gateway

compile-backend-java-distrib:
	make -C "apps/plant_shop_java/Structure_en_application_distribuée/plant_shop_java_distribuée" compile

seed-backend-java-distrib:
	make -C "apps/plant_shop_java/Structure_en_application_distribuée/plant_shop_java_distribuée" seed

pm2-start-backend-java-distrib:
	make -C "apps/plant_shop_java/Structure_en_application_distribuée/plant_shop_java_distribuée" pm2-start-all

pm2-stop-backend-java-distrib:
	make -C "apps/plant_shop_java/Structure_en_application_distribuée/plant_shop_java_distribuée" pm2-stop-all

pm2-start-service-java-distrib:
	@if [ -z "$(SERVICE)" ]; then echo "Usage: make pm2-start-service-java-distrib SERVICE=<nom>"; exit 1; fi
	make -C "apps/plant_shop_java/Structure_en_application_distribuée/plant_shop_java_distribuée" pm2-start SERVICE=$(SERVICE)

pm2-stop-service-java-distrib:
	@if [ -z "$(SERVICE)" ]; then echo "Usage: make pm2-stop-service-java-distrib SERVICE=<nom>"; exit 1; fi
	make -C "apps/plant_shop_java/Structure_en_application_distribuée/plant_shop_java_distribuée" pm2-stop SERVICE=$(SERVICE)

# ======================================================
# ☕ Backend Java Microservices
# ======================================================

run-backend-java-microservices:
	make -C apps/plant_shop_java/Structure_en_micro_services/plant_shop_java_microservices run-gateway

compile-backend-java-microservices:
	make -C apps/plant_shop_java/Structure_en_micro_services/plant_shop_java_microservices compile

seed-backend-java-microservices:
	make -C apps/plant_shop_java/Structure_en_micro_services/plant_shop_java_microservices seed

pm2-start-backend-java-microservices:
	make -C apps/plant_shop_java/Structure_en_micro_services/plant_shop_java_microservices pm2-start-all

pm2-stop-backend-java-microservices:
	make -C apps/plant_shop_java/Structure_en_micro_services/plant_shop_java_microservices pm2-stop-all

pm2-start-service-java-micro:
	@if [ -z "$(SERVICE)" ]; then echo "Usage: make pm2-start-service-java-micro SERVICE=<nom>"; exit 1; fi
	make -C apps/plant_shop_java/Structure_en_micro_services/plant_shop_java_microservices pm2-start SERVICE=$(SERVICE)

pm2-stop-service-java-micro:
	@if [ -z "$(SERVICE)" ]; then echo "Usage: make pm2-stop-service-java-micro SERVICE=<nom>"; exit 1; fi
	make -C apps/plant_shop_java/Structure_en_micro_services/plant_shop_java_microservices pm2-stop SERVICE=$(SERVICE)

# ======================================================
# 🛠️ Backend C (HTTPServer)
# ======================================================

run-backend-c:
	make -C apps/plant_shop_c run

compile-backend-c:
	make -C apps/plant_shop_c compile

seed-backend-c:
	make -C apps/plant_shop_c seed

compile-run-backend-c: compile-backend-c run-backend-c

prod-backend-c:
	make -C apps/plant_shop_c prod

test-backend-c:
	make -C apps/plant_shop_c tests

compile-test-backend-c:
	make -C apps/plant_shop_c compile-test

# ======================================================
# 🌵 Backend Haskell (Stack)
# ======================================================

run-backend-haskell:
	@make -C apps/plant_shop_haskell run

compile-backend-haskell:
	@make -C apps/plant_shop_haskell build

seed-backend-haskell:
	@make -C apps/plant_shop_haskell seed

compile-run-backend-haskell: compile-backend-haskell run-backend-haskell

prod-backend-haskell:
	@make -C apps/plant_shop_haskell build-dev

test-backend-haskell:
	@make -C apps/plant_shop_haskell tests

compile-test-backend-haskell: compile-backend-haskell test-backend-haskell

# ======================================================
# 🐍💻 Backend Python (Flask)
# ======================================================

run-backend-python:
	make -C apps/plant_shop_python run

compile-backend-python:
	@echo "Aucune compilation nécessaire pour Python"

seed-backend-python:
	make -C apps/plant_shop_python seed

compile-run-backend-python: compile-backend-python run-backend-python

prod-backend-python:
	make -C apps/plant_shop_python prod

test-backend-python:
	make -C apps/plant_shop_python tests

compile-test-backend-python: compile-backend-python test-backend-python

# ======================================================
# 🎨 Frontend Angular
# ======================================================

# Run Angular SPA seul (avec proxy backend actif)
run-dev-front:
	npm run dev-front

# Build Frontend Angular
build-front:
	npm run build-frontend

prod-front:
	npm run start-frontend

# ======================================================
# ⚙️ Commun
# ======================================================

# Build global + SSR
build:
	npm run build

# prod: prod-front
# Tempporaire
prod: run-dev-front

build-run-prod: build prod

# Typage / lint
typage:
	npx tsc --noEmit

lint:
	npm run lint

# Seed de la base
seed:
	npx prisma db seed

tests: test-routes

# ======================================================
# 🛠️ Utilitaire
# ======================================================
tree:
	tree -L 6 -I "node_modules" -I "target" -I "uploads" -I "browser" -I "bin" -I "build" -I ".stack-work" -I "venv" -I "__pycache__" -I "lib"

generate_explained_repository:
	node explained_repository_generator.js
