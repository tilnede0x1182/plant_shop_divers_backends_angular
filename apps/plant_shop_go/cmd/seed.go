package main

import (
	"plant_shop_go/internal/db"
	"github.com/joho/godotenv"
)

func main() {
	_ = godotenv.Load(".env")
	conn := db.Connect()
	db.Seed(conn)
}
