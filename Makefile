# Run Dev SSR
run: prod

# Run non-SSR
run-dev-back:
	npm run dev-back

run-dev-front:
	npm run dev-front

# Test des routes Nest
test-e2e:
	npm run test-e2e

test-routes:
	npm run test-routes

# Build et run SSR prod
build:
	npm run build

buid-back:
	npm run build-backend

build-front:
	npm run build-frontend

prod:
	npm run start

build-run-prod: build prod

typage:
	npx tsc --noEmit

seed:
	npx prisma db seed

lint:
	npm run lint
