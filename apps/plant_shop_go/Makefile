
run:
	go run main.go

prod:
	go build -o bin/server main.go && ./bin/server

compile:
	go build -o bin/server main.go

compile_run: compile run

seed:
	go run cmd/seed.go

test:
	go run tests/test_e2e.go

tidy:
	go mod tidy
