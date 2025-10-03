# ======================================================
# 🧩 Backend NestJS
# ======================================================

# Run Nest + Angular SSR
run: prod

# Run Nest backend seul (API, sans SSR Angular)
run-dev-back:
	npm run dev-back

# Tests Nest
test-e2e:
	npm run test-e2e

test-routes:
	npm run test-routes

# Build Nest
buid-back:
	npm run build-backend

# ======================================================
# 🧩 Backend Manifest
# ======================================================

# Run Manifest backend (API seule)
run-manifest:
	nx serve plant_shop_manifest

# Build Manifest backend
build-manifest:
	nx build plant_shop_manifest

# Start Manifest backend (mode prod après build)
prod-manifest:
	nx start plant_shop_manifest

build-prod: build-manifest prod-manifest

# Seed database with Manifest
seed-manifest:
	nx seed plant_shop_manifest

# ======================================================
# 🎨 Frontend Angular
# ======================================================

# Run Angular SPA seul (avec proxy backend actif)
run-dev-front:
	npm run dev-front

# Build Frontend Angular
build-front:
	npm run build-frontend

# ======================================================
# ⚙️ Commun
# ======================================================

# Build global + SSR
build:
	npm run build

prod:
	npm run start

build-run-prod: build prod

# Typage / lint
typage:
	npx tsc --noEmit

lint:
	npm run lint

# Seed de la base
seed:
	npx prisma db seed
