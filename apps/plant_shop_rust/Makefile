run:
	mkdir -p bin
	sqlx database setup
	sqlx migrate run
	cargo build --bin plant_shop_rust
	./target/debug/plant_shop_rust

prod:
	mkdir -p bin
	cargo build --release --bin plant_shop_rust
	sqlx migrate run
	./target/release/plant_shop_rust

compile:
	mkdir -p bin
	cargo build --bin plant_shop_rust

prepare:
	unset SQLX_OFFLINE && cargo sqlx prepare --workspace
seed:
	mkdir -p bin
	sqlx database setup
	sqlx migrate run
	cargo build --bin seed
	./target/debug/seed

test:
	mkdir -p bin
	cargo build --bin test_e2e
	./target/debug/test_e2e
