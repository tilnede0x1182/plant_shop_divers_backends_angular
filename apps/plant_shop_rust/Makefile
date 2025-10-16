run:
	mkdir -p bin
	sqlx database setup
	sqlx migrate run
	cargo build --bin plant_shop_rust --out-dir bin
	./bin/plant_shop_rust

prod:
	mkdir -p bin
	cargo build --release --bin plant_shop_rust --out-dir bin
	sqlx migrate run
	./bin/plant_shop_rust

compile:
	mkdir -p bin
	cargo build --bin plant_shop_rust --out-dir bin

compile_run: compile run

seed:
	mkdir -p bin
	sqlx database setup
	sqlx migrate run
	cargo build --bin seed --out-dir bin
	./bin/seed

test:
	mkdir -p bin
	cargo build --bin test_e2e --out-dir bin
	./bin/test_e2e

clean:
	cargo clean && cargo build
