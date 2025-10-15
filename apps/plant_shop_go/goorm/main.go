package main

import (
	"log"
	"net/http"
	"os"
	"time"

	"github.com/joho/godotenv"
	"github.com/rs/cors"

	"goorm/DB"
	"goorm/internal/db"
	httpserver "goorm/internal/http"
	"goorm/internal/models"
)

func main() {
	// charge .env
	godotenv.Load(".env")

	// connexion et migrations
	conn := db.Connect()
	if err := conn.AutoMigrate(
		&models.User{}, &models.Plant{}, &models.Order{}, &models.OrderItem{},
	); err != nil {
		log.Fatal("migration:", err)
	}

	// seed
	DB.Seed(conn)

	// routeur + CORS
	handler := httpserver.NewRouter()
	c := cors.New(cors.Options{
		AllowedOrigins:   []string{"http://localhost:8300"},
		AllowCredentials: true,
		AllowedMethods:   []string{"GET","POST","PATCH","DELETE","OPTIONS"},
		AllowedHeaders:   []string{"Content-Type"},
	})
	srv := &http.Server{
		Addr:         ":" + getEnv("PORT","4100"),
		Handler:      c.Handler(handler),
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
	}
	log.Printf("listening on %s", srv.Addr)
	log.Fatal(srv.ListenAndServe())
}

func getEnv(key, def string) string {
	if v:=os.Getenv(key); v!="" { return v }
	return def
}
