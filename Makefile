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
# 📜 Backend Manifest ABANDON
# ======================================================

run-proxy-manifest:
	node apps/plant_shop_manifest/proxy.js

# Run Manifest backend (API seule, mode dev)
run-manifest:
	PORT=4120 pnpm run --prefix apps/plant_shop_manifest dev

# Build Manifest backend
build-manifest:
	pnpm run --prefix apps/plant_shop_manifest build

# Start Manifest backend (mode prod)
prod-manifest:
	PORT=4120 pnpm run --prefix apps/plant_shop_manifest start

# Build puis exécution en prod
build-prod: build-manifest prod-manifest

# ======================================================
# 🐹 Backend Go
# ======================================================

run-backend-go:
	go run apps/plant_shop_go/main.go --serve

compile-backend-go:
	go build -o apps/plant_shop_go/bin/server apps/plant_shop_go/main.go

seed-backend-go:
	go run apps/plant_shop_go/cmd/seed.go

compile-run-backend-go: compile-backend-go run-backend-go

prod-backend-go:
	go build -o apps/plant_shop_go/bin/server apps/plant_shop_go/main.go && apps/plant_shop_go/bin/server --serve

# ======================================================
# 🎨 Frontend Angular
# ======================================================

# Run Angular SPA seul (avec proxy backend actif)
run-dev-front:
	npm run dev-front

# Build Frontend Angular
build-front:
	npm run build-frontend

prod-front:
	npm run start-frontend

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

# ======================================================
# 🛠️ Utilitaire
# ======================================================
tree:
	tree -L 6 -I "node_modules" -I "target"
