run:
	mkdir -p bin
	sqlx database setup
	sqlx migrate run
	cargo build --bin plant_shop_rust
	./target/debug/plant_shop_rust

prod:
	cargo build --release --bin plant_shop_rust
	sqlx migrate run
	./target/release/plant_shop_rust

compile:
	cargo build --bin plant_shop_rust

prepare:
	unset SQLX_OFFLINE && cargo sqlx prepare --workspace

create-db:
	sqlx database setup

migrate-db:
	sqlx migrate run

seed:
	sqlx database setup
	sqlx migrate run
	cargo build --bin seed
	./target/debug/seed

test:
	cargo build --bin test_e2e
	./target/debug/test_e2e

tree:
	tree -L 3 -I "node_modules" -I "target"
