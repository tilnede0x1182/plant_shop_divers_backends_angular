package db

// ==============================================================================
// Importations
// ==============================================================================

import (
	"log"
	"os"
	"sync"
	"time"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

// ==============================================================================
// Donnees
// ==============================================================================

var (
	dbConnection     *gorm.DB
	connectionOnce   sync.Once
)

// ==============================================================================
// Fonctions
// ==============================================================================

// Connect renvoie toujours le meme pool GORM et limite les connexions.
//
// @return *gorm.DB Instance de connexion GORM
func Connect() *gorm.DB {
	connectionOnce.Do(func() {
		databaseURL := os.Getenv("DATABASE_URL")
		gormInstance, connectionError := gorm.Open(postgres.Open(databaseURL), &gorm.Config{})
		if connectionError != nil {
			log.Fatalf("DB connection failed: %v", connectionError)
		}
		sqlDatabase, _ := gormInstance.DB()
		sqlDatabase.SetMaxOpenConns(10)
		sqlDatabase.SetMaxIdleConns(5)
		sqlDatabase.SetConnMaxLifetime(time.Hour)
		dbConnection = gormInstance
	})
	return dbConnection
}
