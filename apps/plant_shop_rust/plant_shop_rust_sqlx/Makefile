# ======================================================
# 🚀 Démarrage et compilation de l’API Rust
# ======================================================

run:
	mkdir -p bin
	sqlx database setup
	sqlx migrate run
	cargo build --bin plant_shop_rust
	./target/debug/plant_shop_rust

compile_run: run

prod:
	cargo build --release --bin plant_shop_rust
	sqlx migrate run
	./target/release/plant_shop_rust

compile:
	cargo build --bin plant_shop_rust

# ======================================================
# 🛠️  Outils SQLx et migration
# ======================================================

prepare:
	unset SQLX_OFFLINE && cargo sqlx prepare --workspace

create-db:
	sqlx database setup

migrate-db:
	sqlx migrate run

drop-db:
	sqlx database drop

reset-db:
	sqlx database drop
	sqlx database setup
	sqlx migrate run

# ======================================================
# 🌱 Seed base de données (données de test)
# ======================================================

seed:
	sqlx database setup
	sqlx migrate run
	@if [ -f ./target/release/seed ]; then \
		./target/release/seed; \
	elif [ -f ./target/debug/seed ]; then \
		./target/debug/seed; \
	else \
		cargo build --bin seed; \
		./target/debug/seed; \
	fi

# Build et exécution forcée de la seed en mode debug
seed-dev:
	cargo build --bin seed --manifest-path ./Cargo.toml
	./target/debug/seed

seed-build:
	cargo build --release --bin seed

# ======================================================
# 🧪 Tests End-to-End
# ======================================================

# Lancer les tests : priorité à la version release, puis debug, sinon compilation auto
tests:
	@if [ -f ./target/release/test_e2e ]; then \
		echo "🚀 Lancement des tests (mode RELEASE)…"; \
		./target/release/test_e2e; \
	elif [ -f ./target/debug/test_e2e ]; then \
		echo "🚀 Lancement des tests (mode DEBUG)…"; \
		./target/debug/test_e2e; \
	elif [ -f ./test/test_e2e.rs ]; then \
		echo "⚙️  Compilation du test (mode DEBUG)…"; \
		cargo build --bin test_e2e --manifest-path ./Cargo.toml; \
		echo "🚀 Lancement des tests (mode DEBUG)…"; \
		./target/debug/test_e2e; \
	else \
		echo "❌ Fichier test/test_e2e.rs introuvable."; \
		exit 1; \
	fi

# Compiler les tests en mode release sans les exécuter
test-build:
	@echo "🔧 Compilation du test (mode RELEASE)…"
	cargo build --release --bin test_e2e --manifest-path ./Cargo.toml
	@echo "✅ Binaire disponible : ./target/release/test_e2e"

# Build et exécution forcée des tests end-to-end en mode debug
test-dev:
	cargo build --bin test_e2e --manifest-path ./Cargo.toml
	./target/debug/test_e2e

# ======================================================
# 🌳 Structure du projet (tree)
# ======================================================

tree:
	tree -L 3 -I "node_modules" -I "target"
