# ───────────────────────────────
#   Compilation et exécution principales
# ───────────────────────────────

run:
	./build/server

prod: run

build: clean
	CMAKE_C_COMPILER_LAUNCHER=ccache cmake -S . -B build
	cmake --build build --target server -j$$(nproc)

build-dev: build run

compile:
	if [ ! -d build ]; then cmake -S . -B build; fi
	cmake --build build --target server -j$$(nproc)

compile_run: compile run

clean:
	rm -rf

# ───────────────────────────────
#   Gestion de la seed
# ───────────────────────────────

seed:
	if [ ! -f build/seed_c ]; then \
		CMAKE_C_COMPILER_LAUNCHER=ccache cmake -S . -B build; \
	fi; \
	cmake --build build --target seed_c -j$$(nproc); \
	./build/seed_c

compile-seed:
	if [ ! -d build ]; then cmake -S . -B build; fi
	cmake --build build --target seed_c -j$$(nproc)

seed-dev: compile-seed seed

seed-build:
	CMAKE_C_COMPILER_LAUNCHER=ccache cmake -S . -B build
	cmake --build build --target seed_c -j$$(nproc)

# ───────────────────────────────
#   Gestion du test end-to-end
# ───────────────────────────────

tests:
	if [ ! -f build/test_e2e_c ]; then \
		CMAKE_C_COMPILER_LAUNCHER=ccache cmake -S . -B build; \
		cmake --build build --target test_e2e_c -j$$(nproc); \
	fi; \
	./build/test_e2e_c

compile-test:
	if [ ! -d build ]; then cmake -S . -B build; fi
	cmake --build build --target test_e2e_c -j$$(nproc)

test-dev: compile-test tests

test-build:
	CMAKE_C_COMPILER_LAUNCHER=ccache cmake -S . -B build
	cmake --build build --target test_e2e_c -j$$(nproc)

# ───────────────────────────────
#   Commandes Base de données
# ───────────────────────────────

# Extrait la valeur de DATABASE_URL du fichier .env
DB_NAME := $(shell grep '^DATABASE_URL=' .env | cut -d'=' -f2 | cut -d'#' -f1 | xargs)

db-create:
	sudo -u postgres psql -c "CREATE DATABASE $(DB_NAME)" || echo "↳ base déjà existante."

db-migrate:
	sudo -u postgres psql -d $(DB_NAME) -f db/schema.sql

db-drop:
	sudo -u postgres psql -c "DROP DATABASE IF EXISTS $(DB_NAME);"

db-reset: db-drop db-create db-migrate

# ───────────────────────────────
#   Utilitaires
# ───────────────────────────────

tree:
	tree . -I "kore" -I "CMakeFiles"
