package main

import (
	"log"
	"net/http"
	"os"
	"time"

	"github.com/joho/godotenv"
	"github.com/rs/cors"

	"plant_shop_go/internal/db"
	httpserver "plant_shop_go/internal/http"
	"plant_shop_go/internal/models"
)

// main est le point d entrée du serveur.
// Initialise la base de données et démarre le serveur HTTP.
func main() {
	_ = godotenv.Load(".env")

	conn := db.Connect()
	if conn == nil {
		log.Fatal("db connection failed")
	}

	if err := conn.AutoMigrate(
		&models.User{},
		&models.Plant{},
		&models.Order{},
		&models.OrderItem{},
	); err != nil {
		log.Fatalf("migration failed: %v", err)
	}

	// Seed retiré volontairement. Utilisez ./cmd/seed pour lancer le seed.

	router := httpserver.NewRouter(conn)
	c := cors.New(cors.Options{
		AllowedOrigins:   []string{"http://localhost:8300"},
		AllowCredentials: true,
		AllowedMethods:   []string{"GET", "POST", "PATCH", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"Content-Type"},
	})
	srv := &http.Server{
		Addr:         ":" + getEnv("PORT", "4100"),
		Handler:      c.Handler(router),
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
	}

	log.Printf("listening on %s", srv.Addr)
	log.Fatal(srv.ListenAndServe())
}

// getEnv récupère une variable d environnement ou retourne une valeur par défaut.
// @param key Nom de la variable d environnement
// @param def Valeur par défaut
func getEnv(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}
