
# Server

compile:
	mkdir -p bin
	find src -type f -name "*.java" ! -name "Seed.java" ! -name "Test.java" | xargs javac -d bin

run:
	java -cp bin Main

compile-run: compile run

# DB

db-create:
	sudo psql -U postgres -c "CREATE DATABASE plant_shop_java;"

db-migrate:
	sudo psql -U postgres -d plant_shop_java -f src/db/schema.sql

db-drop:
	sudo psql -U postgres -c "DROP DATABASE plant_shop_java;"

db-seed:
	# Exemple SQL direct (remplace ou adapte selon ton projet)
	sudo psql -U postgres -d plant_shop_java -f src/db/seed.sql

db-reset: db-drop db-create db-migrate db-seed

# Seed

compile-seed:
	mkdir -p bin
	javac -d bin src/db/Seed.java

seed: compile-seed
	java -cp bin db.Seed

seed-dev: compile-seed seed

# Test

compile-test:
	mkdir -p bin
	javac -d bin Tests/test.java

tests: compile-test
	java -cp bin Tests.test

test-dev: compile-test tests
