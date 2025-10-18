// tests/test_e2e.go
package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/http/cookiejar"
	"os"
	"strings"
	"time"
)

/* ------- Variables globales -------- */
var (
	cookieJars = make(map[string]string)
	maintenant = time.Now().Format("20060102150405")
	jar, _     = cookiejar.New(nil)
	client     = &http.Client{Jar: jar}
)

/* ---------- Configuration ---------- */
type Config struct {
	ApiBaseUrl    string
	LogLevel      string // "silent", "normal", "verbose"
	AdminEmail    string
	AdminPassword string
}

var config = Config{
	ApiBaseUrl:    getEnv("API_BASE_URL", "http://localhost:4100/api"),
	LogLevel:      "verbose",
	AdminEmail:    getEnv("ADMIN_EMAIL", "admin1@planteshop.com"),
	AdminPassword: getEnv("ADMIN_PASSWORD", "password"),
}

func getEnv(key, fallback string) string {
	if value, ok := os.LookupEnv(key); ok {
		return value
	}
	return fallback
}

/* ---------- Utilitaires HTTP ---------- */

// hit est pour les endpoints qui retournent un objet JSON { ... }
func hit(method, route string, expectedStatus int, body any, who string) map[string]any {
	res, err := doRequest(method, route, expectedStatus, body, who)
	if err != nil {
		log.Fatalf("Request failed: %v", err)
	}
	defer res.Body.Close()

	// *** LA CORRECTION EST ICI ***
	// Si on s'attend à un statut d'erreur (>=400), on ne tente pas de parser le corps.
	// Le test a réussi simplement en obtenant le bon code de statut.
	if expectedStatus >= 400 {
		return make(map[string]any)
	}

	if res.ContentLength == 0 {
		return make(map[string]any)
	}

	var result map[string]any
	bodyBytes, _ := io.ReadAll(res.Body) // Lire le corps pour le débogage
	if err := json.Unmarshal(bodyBytes, &result); err != nil {
		log.Fatalf("Failed to decode JSON object from %s %s: %v. Body: %s", method, route, err, string(bodyBytes))
	}
	return result
}

// hitList est pour les endpoints qui retournent un tableau JSON [ ... ]
func hitList(method, route string, expectedStatus int, body any, who string) []map[string]any {
	res, err := doRequest(method, route, expectedStatus, body, who)
	if err != nil {
		log.Fatalf("Request failed: %v", err)
	}
	defer res.Body.Close()

	// *** LA CORRECTION EST ICI ***
	// Idem pour les listes : pas de parsing sur les codes d'erreur attendus.
	if expectedStatus >= 400 {
		return make([]map[string]any, 0)
	}

	if res.ContentLength == 0 {
		return make([]map[string]any, 0)
	}

	var result []map[string]any
	bodyBytes, _ := io.ReadAll(res.Body) // Lire le corps pour le débogage
	if err := json.Unmarshal(bodyBytes, &result); err != nil {
		log.Fatalf("Failed to decode JSON array from %s %s: %v. Body: %s", method, route, err, string(bodyBytes))
	}
	return result
}

// doRequest est le moteur de requête interne
func doRequest(method, route string, expectedStatus int, body any, who string) (*http.Response, error) {
	url := config.ApiBaseUrl + route
	label := fmt.Sprintf("%s %s", method, route)

	var reqBody io.Reader
	if body != nil {
		jsonBody, err := json.Marshal(body)
		if err != nil {
			return nil, fmt.Errorf("failed to marshal body: %w", err)
		}
		reqBody = bytes.NewBuffer(jsonBody)
	}

	req, err := http.NewRequest(method, url, reqBody)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")
	if cookie, ok := cookieJars[who]; ok && cookie != "" {
		req.Header.Set("Cookie", cookie)
	}

	res, err := client.Do(req)
	if err != nil {
		fmt.Printf("❌ Connection error: %s - API down ?\n", url)
		return nil, fmt.Errorf("failed to execute request: %w", err)
	}

	if setCookie := res.Header.Get("Set-Cookie"); setCookie != "" {
		cookieJars[who] = strings.Split(setCookie, ";")[0]
	}

	success := res.StatusCode == expectedStatus
	if config.LogLevel != "silent" {
		log.Printf("%s %s [%d]", map[bool]string{true: "✅", false: "❌"}[success], label, res.StatusCode)
	}

	if !success {
		resBody, _ := io.ReadAll(res.Body)
		res.Body.Close()
		return nil, fmt.Errorf("API %s → %d (attendu %d)\n%s", label, res.StatusCode, expectedStatus, string(resBody))
	}

	return res, nil
}

/* ---------- Assertions ---------- */
func assertEq(obj map[string]any, key string, expected any) {
	actual, ok := obj[key]
	if !ok {
		log.Fatalf("Assertion failed: key '%s' not found in object", key)
	}

	isEqual := fmt.Sprintf("%v", actual) == fmt.Sprintf("%v", expected)

	if config.LogLevel != "silent" {
		log.Printf("%s   ↳ %s=%v (attendu %v)", map[bool]string{true: "✅", false: "❌"}[isEqual], key, actual, expected)
	}
	if !isEqual {
		log.Fatalf("Assertion failed: %s=%v, attendu %v", key, actual, expected)
	}
}

/* ------------ Helpers ------------ */
func login(email, password, who string) {
	hit("POST", "/auth/login", 201, map[string]string{"email": email, "password": password}, who)
}

func registerUser(name, email, password, who string) {
	hit("POST", "/auth/register", 201, map[string]string{"name": name, "email": email, "password": password}, who)
}

func findUserIDByEmail(who, email string) int {
	users := hitList("GET", "/users", 200, nil, who)
	for _, u := range users {
		if u["email"] == email {
			return int(u["id"].(float64))
		}
	}
	log.Fatalf("User %s not found in admin list", email)
	return 0
}

/* ---------- Modules de test ---------- */
func testPlants(who string) {
	log.Println("\n📌 TEST MODULE: PLANTS (admin)")
	plantData := map[string]any{"name": "Test Plant", "price": 10, "stock": 5}

	createdPlant := hit("POST", "/admin/plants", 201, plantData, who)
	plantID := int(createdPlant["id"].(float64))

	fetchedPlant := hit("GET", fmt.Sprintf("/plants/%d", plantID), 200, nil, who)
	assertEq(fetchedPlant, "name", plantData["name"])

	hit("PATCH", fmt.Sprintf("/admin/plants/%d", plantID), 200, map[string]int{"price": 15}, who)
	updatedPlant := hit("GET", fmt.Sprintf("/plants/%d", plantID), 200, nil, who)
	assertEq(updatedPlant, "price", 15)

	hit("DELETE", fmt.Sprintf("/admin/plants/%d", plantID), 200, nil, who)
}

func testUsers(who string) {
	log.Println("\n📌 TEST MODULE: USERS (admin)")
	userData := map[string]string{
		"email":    fmt.Sprintf("utilisateur_test_%s@example.com", maintenant),
		"name":     fmt.Sprintf("Utilisateur de test %s", maintenant),
		"password": "pass123",
	}
	createdUser := hit("POST", "/users", 201, userData, who)
	userID := int(createdUser["id"].(float64))

	hit("PATCH", fmt.Sprintf("/users/%d", userID), 200, map[string]string{"name": "Tester Update"}, who)
	updatedUser := hit("GET", fmt.Sprintf("/users/%d", userID), 200, nil, who)
	assertEq(updatedUser, "name", "Tester Update")

	hit("DELETE", fmt.Sprintf("/users/%d", userID), 200, nil, who)
}

func testOrders(adminWho, userWho string) {
	log.Println("\n📌 TEST MODULE: ORDERS & ORDER ITEMS")

	plantData := map[string]any{"name": fmt.Sprintf("Plante_de_test_%s", maintenant), "price": 10, "stock": 5}
	createdPlant := hit("POST", "/admin/plants", 201, plantData, adminWho)
	plantID := int(createdPlant["id"].(float64))

	orderPayload := map[string]any{
		"items": []map[string]any{
			{"plantId": plantID, "quantity": 2},
		},
	}
	createdOrder := hit("POST", "/orders", 201, orderPayload, userWho)
	orderID := int(createdOrder["id"].(float64))

	hit("PATCH", fmt.Sprintf("/orders/%d", orderID), 200, map[string]string{"status": "shipped"}, adminWho)

	orders := hitList("GET", "/orders", 200, nil, userWho)
	var foundOrder map[string]any
	for _, o := range orders {
		if int(o["id"].(float64)) == orderID {
			foundOrder = o
			break
		}
	}
	if foundOrder == nil {
		log.Fatalf("Commande %d introuvable", orderID)
	}
	assertEq(foundOrder, "status", "shipped")

	hit("DELETE", fmt.Sprintf("/orders/%d", orderID), 200, nil, adminWho)
	hit("DELETE", fmt.Sprintf("/admin/plants/%d", plantID), 200, nil, adminWho)
}

func testUserProfile(adminWho, userWho, userEmail string) {
	log.Println("\n📌 TEST MODULE: USER PROFILE (user)")
	userID := findUserIDByEmail(adminWho, userEmail)

	profile := hit("GET", fmt.Sprintf("/users/%d", userID), 200, nil, userWho)
	assertEq(profile, "id", userID)

	nouveauNom := fmt.Sprintf("Utilisateur_de_test_%s", maintenant)
	hit("PATCH", fmt.Sprintf("/users/%d", userID), 200, map[string]string{"name": nouveauNom}, userWho)
	updatedProfile := hit("GET", fmt.Sprintf("/users/%d", userID), 200, nil, userWho)
	assertEq(updatedProfile, "name", nouveauNom)

	hit("PATCH", fmt.Sprintf("/users/%d", userID), 200, map[string]bool{"admin": true}, userWho)
	finalProfile := hit("GET", fmt.Sprintf("/users/%d", userID), 200, nil, adminWho)
	assertEq(finalProfile, "admin", false)
}

func testAuthRoles(adminWho, userWho string) {
	log.Println("\n📌 TEST MODULE: ROLES")

	hit("POST", "/admin/plants", 403, map[string]any{"name": "Bad", "price": 1, "stock": 1}, userWho)

	goodPlant := hit("POST", "/admin/plants", 201, map[string]any{"name": "Good", "price": 1, "stock": 1}, adminWho)
	pid := int(goodPlant["id"].(float64))
	hit("DELETE", fmt.Sprintf("/admin/plants/%d", pid), 200, nil, adminWho)

	hitList("GET", "/users", 403, nil, userWho)
}

func testAdminPlants(who string) {
	log.Println("\n📌 TEST MODULE: ADMIN PLANTS")
	plantes := hitList("GET", "/admin/plants", 200, nil, who)
	log.Printf("   ↳ %d plantes récupérées", len(plantes))

	d := map[string]any{"name": fmt.Sprintf("Plante_admin_de_test_%s", maintenant), "price": 99, "stock": 12}
	createdPlant := hit("POST", "/admin/plants", 201, d, who)
	id := int(createdPlant["id"].(float64))
	hit("PATCH", `/admin/plants/`+fmt.Sprintf("%d", id), 200, map[string]int{"price": 123}, who)
	hit("DELETE", `/admin/plants/`+fmt.Sprintf("%d", id), 200, nil, who)
}

func testAdminUsers(who string) {
	log.Println("\n📌 TEST MODULE: ADMIN USERS")
	utilisateurs := hitList("GET", "/admin/users", 200, nil, who)
	log.Printf("   ↳ %d utilisateurs récupérés", len(utilisateurs))

	if len(utilisateurs) == 0 {
		log.Println("   ↳ Pas d'utilisateurs à tester, skip.")
		return
	}
	u := utilisateurs[0]
	uid := int(u["id"].(float64))
	nomModifie := fmt.Sprintf("Admin_de_test_modifie_%s", maintenant)
	hit("PATCH", fmt.Sprintf("/admin/users/%d", uid), 200, map[string]string{"name": nomModifie}, who)

	updatedUser := hit("GET", fmt.Sprintf("/users/%d", uid), 200, nil, who)
	assertEq(updatedUser, "name", nomModifie)
}

func testAuthMe(who string) {
	log.Println("\n📌 TEST MODULE: AUTH /me")
	me := hit("GET", "/auth/me", 200, nil, who)
	if me["email"] == nil {
		log.Fatalf("Réponse invalide pour /auth/me")
	}
	log.Printf("   ↳ Utilisateur connecté: %s (%s)", me["email"], me["name"])
}

/* ---------- Exécution des tests ---------- */
func main() {
	defer func() {
		if r := recover(); r != nil {
			fmt.Fprintf(os.Stderr, "\n❌ Tests interrompus: %v\n", r)
			os.Exit(1)
		}
	}()

	log.Printf("🧪 Démarrage des tests: %s\n", config.ApiBaseUrl)

	login(config.AdminEmail, config.AdminPassword, "admin")
	userEmail := fmt.Sprintf("utilisateur_de_test_%s@example.com", maintenant)
	registerUser("User", userEmail, "pass123", "user")
	login(userEmail, "pass123", "user")

	testPlants("admin")
	testUsers("admin")
	testOrders("admin", "user")
	testUserProfile("admin", "user", userEmail)
	testAuthRoles("admin", "user")
	testAdminPlants("admin")
	testAdminUsers("admin")
	testAuthMe("user")

	log.Println("\n🎉 Tous les tests ont réussi!")
}
