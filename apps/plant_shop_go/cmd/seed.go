package main

import (
	"log"

	"github.com/joho/godotenv"
	"gorm.io/gorm"

	"plant_shop_go/internal/db"
	"plant_shop_go/internal/models"
)

// migrateSchema cree le schema des tables.
//
// @param conn *gorm.DB Connexion DB
func migrateSchema(conn *gorm.DB) {
	log.Println("Creation du schema des tables...")
	conn.AutoMigrate(&models.User{}, &models.Plant{}, &models.Order{}, &models.OrderItem{})
	log.Println("Schema cree avec succes.")
}

// main est le point d entree du seed.
func main() {
	_ = godotenv.Load(".env")
	conn := db.Connect()
	migrateSchema(conn)
	db.Seed(conn)
}
