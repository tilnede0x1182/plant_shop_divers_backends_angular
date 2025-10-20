# ───────────────────────────────
#   Compilation et exécution principales
# ───────────────────────────────

compile:
	cmake -S . -B build
	cmake --build build --target plant_shop_cpp

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
		cmake -S . -B build; \
		cmake --build build --target seed_cpp; \
	fi; \
	./build/seed_cpp

seed_dev:
	cmake -S . -B build
	cmake --build build --target seed_cpp
	./build/seed_cpp

seed-build:
	cmake -S . -B build
	cmake --build build --target seed_cpp

# ───────────────────────────────
#   Gestion du test end-to-end
# ───────────────────────────────

test:
	if [ ! -f build/test_e2e_cpp ]; then \
		cmake -S . -B build; \
		cmake --build build --target test_e2e_cpp; \
	fi; \
	./build/test_e2e_cpp

test-dev:
	cmake -S . -B build
	cmake --build build --target test_e2e_cpp
	./build/test_e2e_cpp

test-build:
	cmake -S . -B build
	cmake --build build --target test_e2e_cpp
