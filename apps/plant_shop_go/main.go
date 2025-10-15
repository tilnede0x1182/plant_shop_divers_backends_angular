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

func main() {
	_ = godotenv.Load(".env")

	conn := db.Connect()
	if err := conn.AutoMigrate(
		&models.User{},
		&models.Plant{},
		&models.Order{},
		&models.OrderItem{},
	); err != nil {
		log.Fatalf("migration failed: %v", err)
	}

	db.Seed(conn)

	router := httpserver.NewRouter()
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

func getEnv(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}
