# ───────────────────────────────
#   Compilation et exécution principales
# ───────────────────────────────

compile:
	CMAKE_CXX_COMPILER_LAUNCHER=ccache cmake -S . -B build
	cmake --build build --target plant_shop_cpp -j$$(nproc)

run:
	./build/plant_shop_cpp

compile_run:
	make compile
	make run

# ───────────────────────────────
#   Gestion de la seed
# ───────────────────────────────

seed:
	if [ ! -f build/seed_cpp ]; then \
		CMAKE_CXX_COMPILER_LAUNCHER=ccache cmake -S . -B build; \
	fi; \
	cmake --build build --target seed_cpp -j$$(nproc); \
	./build/seed_cpp

seed_dev:
	CMAKE_CXX_COMPILER_LAUNCHER=ccache cmake -S . -B build
	cmake --build build --target seed_cpp -j$$(nproc)
	./build/seed_cpp

seed_build:
	CMAKE_CXX_COMPILER_LAUNCHER=ccache cmake -S . -B build
	cmake --build build --target seed_cpp -j$$(nproc)

# ───────────────────────────────
#   Gestion du test end-to-end
# ───────────────────────────────

test:
	if [ ! -f build/test_e2e_cpp ]; then \
		CMAKE_CXX_COMPILER_LAUNCHER=ccache cmake -S . -B build; \
	fi; \
	cmake --build build --target test_e2e_cpp -j$$(nproc); \
	./build/test_e2e_cpp

test_dev:
	CMAKE_CXX_COMPILER_LAUNCHER=ccache cmake -S . -B build
	cmake --build build --target test_e2e_cpp -j$$(nproc)
	./build/test_e2e_cpp

test_build:
	CMAKE_CXX_COMPILER_LAUNCHER=ccache cmake -S . -B build
	cmake --build build --target test_e2e_cpp -j$$(nproc)

# ───────────────────────────────
#   Commandes Base de données
# ───────────────────────────────

db-create:
	sudo -u postgres psql -c "CREATE DATABASE $$(grep DATABASE_URL .env | cut -d'/' -f4)" || echo "↳ base déjà existante."

db-migrate:
	sudo -u postgres psql -d $$(grep DATABASE_URL .env | cut -d'/' -f4) -f db/Migrations.sql

db-drop:
	sudo -u postgres psql -c "DROP DATABASE IF EXISTS $$(grep DATABASE_URL .env | cut -d'/' -f4);"

db-reset: db-drop db-create db-migrate
