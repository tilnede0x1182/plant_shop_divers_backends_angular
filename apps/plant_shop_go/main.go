package main

// ==============================================================================
// Importations
// ==============================================================================

import (
	"log"
	"net/http"
	"os"
	"time"

	"github.com/joho/godotenv"
	"github.com/rs/cors"
	"gorm.io/gorm"

	"plant_shop_go/internal/db"
	httpserver "plant_shop_go/internal/http"
	"plant_shop_go/internal/models"
)

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

// getEnv recupere une variable d environnement ou retourne une valeur par defaut.
//
// @param envKey string Nom de la variable d environnement
// @param defaultValue string Valeur par defaut
// @return string Valeur trouvee ou defaut
func getEnv(envKey, defaultValue string) string {
	if envValue := os.Getenv(envKey); envValue != "" {
		return envValue
	}
	return defaultValue
}

// initDatabase initialise la connexion et les migrations.
//
// @return *gorm.DB Connexion DB
func initDatabase() *gorm.DB {
	gormDB := db.Connect()
	if gormDB == nil {
		log.Fatal("db connection failed")
	}
	migrateModels(gormDB)
	return gormDB
}

// migrateModels execute les migrations automatiques.
//
// @param gormDB *gorm.DB Connexion DB
func migrateModels(gormDB *gorm.DB) {
	migrationError := gormDB.AutoMigrate(&models.User{}, &models.Plant{}, &models.Order{}, &models.OrderItem{})
	if migrationError != nil {
		log.Fatalf("migration failed: %v", migrationError)
	}
}

// ------------------------------------------------------------------------------
// Configuration serveur
// ------------------------------------------------------------------------------

// createCorsHandler configure le middleware CORS.
//
// @return *cors.Cors Handler CORS configure
func createCorsHandler() *cors.Cors {
	return cors.New(cors.Options{
		AllowedOrigins:   []string{"http://localhost:8300"},
		AllowCredentials: true,
		AllowedMethods:   []string{"GET", "POST", "PATCH", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"Content-Type"},
	})
}

// createServer configure le serveur HTTP.
//
// @param handler http.Handler Handler principal
// @return *http.Server Serveur configure
func createServer(handler http.Handler) *http.Server {
	return &http.Server{
		Addr:         ":" + getEnv("PORT", "4100"),
		Handler:      handler,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
	}
}

// ==============================================================================
// Main
// ==============================================================================

// main est le point d entree du serveur.
func main() {
	_ = godotenv.Load(".env")
	gormDB := initDatabase()
	mainRouter := httpserver.NewRouter(gormDB)
	corsMiddleware := createCorsHandler()
	httpServer := createServer(corsMiddleware.Handler(mainRouter))
	log.Printf("listening on %s", httpServer.Addr)
	log.Fatal(httpServer.ListenAndServe())
}
