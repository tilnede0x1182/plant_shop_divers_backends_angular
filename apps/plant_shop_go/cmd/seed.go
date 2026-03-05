package main

// ==============================================================================
// Importations
// ==============================================================================

import (
	"log"

	"github.com/joho/godotenv"
	"gorm.io/gorm"

	"plant_shop_go/internal/db"
	"plant_shop_go/internal/models"
)

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

// migrateSchema cree le schema des tables.
//
// @param dbConnection *gorm.DB Connexion a la base de donnees
func migrateSchema(dbConnection *gorm.DB) {
	log.Println("Creation du schema des tables...")
	dbConnection.AutoMigrate(&models.User{}, &models.Plant{}, &models.Order{}, &models.OrderItem{})
	log.Println("Schema cree avec succes.")
}

// ==============================================================================
// Main
// ==============================================================================

// main est le point d entree du seed.
func main() {
	_ = godotenv.Load(".env")
	dbConnection := db.Connect()
	migrateSchema(dbConnection)
	db.Seed(dbConnection)
}
