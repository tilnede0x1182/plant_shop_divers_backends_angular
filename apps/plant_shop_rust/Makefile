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

seed-build:
	cargo build --release --bin seed

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

# ======================================================
# 🧪 Tests End-to-End
# ======================================================

test:
	cargo build --bin test_e2e
	./target/debug/test_e2e

# ======================================================
# 🌳 Structure du projet (tree)
# ======================================================

tree:
	tree -L 3 -I "node_modules" -I "target"
