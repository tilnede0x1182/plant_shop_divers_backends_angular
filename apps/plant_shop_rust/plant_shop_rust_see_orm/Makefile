# ======================================================
# 🚀 Démarrage et compilation de l’API Rust
# ======================================================

run:
	mkdir -p bin
	cargo run --bin plant_shop_rust

compile_run: run

prod:
	cargo build --release --bin plant_shop_rust
	./target/release/plant_shop_rust

compile:
	cargo build --bin plant_shop_rust

# ======================================================
# 🛠️  Gestion des migrations locales (Rust)
# ======================================================

create-db:
	@echo "📦 Création de la base PostgreSQL si nécessaire..."
	sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='plant_shop_rust_see_orm';" | grep -q 1 || \
	sudo -u postgres psql -c "CREATE DATABASE plant_shop_rust_see_orm;"
	cargo run --bin plant_shop_rust -- migrate create

migrate-db:
	cargo run --bin plant_shop_rust -- migrate up

drop-db:
	@echo "⚠️ Suppression de la base PostgreSQL..."
	sudo -u postgres psql -c "DROP DATABASE IF EXISTS plant_shop_rust_see_orm;"
	cargo run --bin plant_shop_rust -- migrate drop || true

reset-db:
	@echo "🔁 Réinitialisation complète de la base..."
	sudo -u postgres psql -c "DROP DATABASE IF EXISTS plant_shop_rust_see_orm;"
	sudo -u postgres psql -c "CREATE DATABASE plant_shop_rust_see_orm;"
	cargo run --bin plant_shop_rust -- migrate reset

# ======================================================
# 🌱 Seed base de données
# ======================================================

seed:
	@echo "🌱 Lancement de la seed..."
	@if [ -f ./target/release/seed ]; then \
		./target/release/seed; \
	elif [ -f ./target/debug/seed ]; then \
		./target/debug/seed; \
	else \
		cargo build --bin seed; \
		./target/debug/seed; \
	fi

seed-dev:
	cargo build --bin seed --manifest-path ./Cargo.toml
	./target/debug/seed

seed-build:
	cargo build --release --bin seed

# ======================================================
# 🧪 Tests End-to-End
# ======================================================

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

test-build:
	@echo "🔧 Compilation du test (mode RELEASE)…"
	cargo build --release --bin test_e2e --manifest-path ./Cargo.toml
	@echo "✅ Binaire disponible : ./target/release/test_e2e"

test-dev:
	cargo build --bin test_e2e --manifest-path ./Cargo.toml
	./target/debug/test_e2e

# ======================================================
# 🌳 Structure du projet (tree)
# ======================================================

tree:
	tree -L 3 -I "node_modules" -I "target"
