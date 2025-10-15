package tests

// Suite E2E : enregistre un utilisateur, récupère le cookie JWT,
// vérifie l’accès protégé, crée une commande
// puis effectue un CRUD complet sur la même base Postgres
// définie par DATABASE_URL (.env).

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"

	"github.com/joho/godotenv"
	httpserver "goorm/internal/http"
	"goorm/internal/db"
	"goorm/internal/models"
)

func TestE2E(t *testing.T) {
	_ = godotenv.Load("../../.env")        // charge JWT_SECRET et DATABASE_URL
	os.Setenv("JWT_SECRET", "testsecret") // fixe un secret de test

	srv := httptest.NewServer(httpserver.NewRouter())
	defer srv.Close()

	cookie := register(t, srv.URL)
	checkAuth(t, srv.URL, cookie)
	checkUnauthorized(t, srv.URL)
	createOrder(t, srv.URL, cookie)
	crudPostgres(t)
}

/* ---------- API helpers ---------- */

func register(t *testing.T, base string) *http.Cookie {
	body, _ := json.Marshal(map[string]string{
		"email":    fmt.Sprintf("test_%d@example.com", time.Now().UnixNano()),
		"password": "123456",
	})
	res, err := http.Post(base+"/auth/register", "application/json", bytes.NewBuffer(body))
	if err != nil || res.StatusCode != http.StatusOK {
		t.Fatalf("register → status=%d err=%v", res.StatusCode, err)
	}
	if len(res.Cookies()) == 0 {
		t.Fatalf("register → aucun cookie reçu")
	}
	return res.Cookies()[0]
}

func checkAuth(t *testing.T, base string, c *http.Cookie) {
	req, _ := http.NewRequest("GET", base+"/users/me", nil)
	req.AddCookie(c)
	res, _ := http.DefaultClient.Do(req)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("/users/me → attendu 200, reçu %d", res.StatusCode)
	}
}

func checkUnauthorized(t *testing.T, base string) {
	res, _ := http.Get(base + "/users/me")
	if res.StatusCode != http.StatusUnauthorized {
		t.Fatalf("/users/me sans cookie → attendu 401, reçu %d", res.StatusCode)
	}
}

func createOrder(t *testing.T, base string, c *http.Cookie) {
	body, _ := json.Marshal(map[string]any{
		"items": []map[string]any{{"plantId": "p1", "qty": 2}},
	})
	req, _ := http.NewRequest("POST", base+"/orders", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	req.AddCookie(c)
	res, _ := http.DefaultClient.Do(req)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("/orders → attendu 200, reçu %d", res.StatusCode)
	}
}

/* ---------- DB CRUD ---------- */

func crudPostgres(t *testing.T) {
	database := db.Connect() // même DATABASE_URL que le serveur

	plantName := fmt.Sprintf("TestPlant_%d", time.Now().UnixNano())
	p := models.Plant{Name: plantName, Price: 999, Stock: 3}
	if err := database.Create(&p).Error; err != nil {
		t.Fatalf("create plant → %v", err)
	}

	var out models.Plant
	if err := database.First(&out, p.ID).Error; err != nil {
		t.Fatalf("read plant → %v", err)
	}

	if err := database.Model(&out).Update("name", plantName+"_upd").Error; err != nil {
		t.Fatalf("update plant → %v", err)
	}

	if err := database.Delete(&models.Plant{}, p.ID).Error; err != nil {
		t.Fatalf("delete plant → %v", err)
	}
}
