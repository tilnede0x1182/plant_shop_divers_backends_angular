package main

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

// getEnv recupere une variable d environnement ou retourne une valeur par defaut.
//
// @param key string Nom de la variable d environnement
// @param def string Valeur par defaut
// @return string Valeur trouvee ou defaut
func getEnv(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

// initDatabase initialise la connexion et les migrations.
//
// @return *gorm.DB Connexion DB
func initDatabase() *gorm.DB {
	conn := db.Connect()
	if conn == nil {
		log.Fatal("db connection failed")
	}
	migrateModels(conn)
	return conn
}

// migrateModels execute les migrations automatiques.
//
// @param conn *gorm.DB Connexion DB
func migrateModels(conn *gorm.DB) {
	err := conn.AutoMigrate(&models.User{}, &models.Plant{}, &models.Order{}, &models.OrderItem{})
	if err != nil {
		log.Fatalf("migration failed: %v", err)
	}
}

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

// main est le point d entree du serveur.
func main() {
	_ = godotenv.Load(".env")
	conn := initDatabase()
	router := httpserver.NewRouter(conn)
	corsHandler := createCorsHandler()
	srv := createServer(corsHandler.Handler(router))
	log.Printf("listening on %s", srv.Addr)
	log.Fatal(srv.ListenAndServe())
}
