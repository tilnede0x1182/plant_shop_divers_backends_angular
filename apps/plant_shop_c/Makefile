# ───────────────────────────────
#   Compilation et exécution principales
# ───────────────────────────────

run:
	./build/server

prod: run

build-dev:
	CMAKE_C_COMPILER_LAUNCHER=ccache cmake -S . -B build
	cmake --build build --target server -j$$(nproc)

compile:
	if [ ! -d build ]; then cmake -S . -B build; fi
	cmake --build build --target server -j$$(nproc)

compile_run: compile run

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

# Cible principale : compile si nécessaire, puis lance les tests.
# C'est la commande à utiliser la plupart du temps.
test: run-test

# Lance les tests (dépend de la compilation pour s'assurer que l'exécutable est à jour).
run-test: compile-test
	./build/test_e2e_c

# Compile uniquement l'exécutable de test.
compile-test:
	# Configure cmake la première fois si le dossier build n'existe pas
	if [ ! -d build ]; then cmake -S . -B build; fi
	# Construit la cible de test
	cmake --build build --target test_e2e_c -j$$(nproc)

# Re-configure et re-compile proprement les tests (utile après un changement de CMakeLists.txt)
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
	tree . -I "Kore" -I "CMakeFiles"
