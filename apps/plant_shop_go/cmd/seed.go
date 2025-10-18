package main

import (
	"plant_shop_go/internal/db"
	"plant_shop_go/internal/models"
	"github.com/joho/godotenv"
	"log"
)

func main() {
	_ = godotenv.Load(".env")

	conn := db.Connect()

	log.Println("🧱 Création du schéma des tables…")
	conn.AutoMigrate(
		&models.User{},
		&models.Plant{},
		&models.Order{},
		&models.OrderItem{},
	)
	log.Println("✅ Schéma créé avec succès.")

	db.Seed(conn)
}
