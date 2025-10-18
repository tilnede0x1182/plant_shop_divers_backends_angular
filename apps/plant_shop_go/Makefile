
run:
	go run main.go

prod:
	go build -o bin/server main.go && ./bin/server

compile:
	go build -o bin/server main.go

compile_run: compile run

tidy:
	go mod tidy

seed:
	go run cmd/seed.go

db-reset:
	psql -U tilnede0x1182 -d postgres -c "DROP DATABASE IF EXISTS plant_shop_go;"
	psql -U tilnede0x1182 -d postgres -c "CREATE DATABASE plant_shop_go;"
	go run cmd/seed.go

tests:
	go run test/test_e2e.go
