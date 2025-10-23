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
	"unicode"
	"golang.org/x/text/unicode/norm"
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

	bodyBytes, _ := io.ReadAll(res.Body)
	if len(bodyBytes) == 0 {
		return make(map[string]any)
	}

	var result map[string]any
	if err := json.Unmarshal(bodyBytes, &result); err != nil {
		// Aligné sur test_complet.js : JSON manquant ou invalide → objet vide
		return make(map[string]any)
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

	bodyBytes, _ := io.ReadAll(res.Body)
	if len(bodyBytes) == 0 {
		return make([]map[string]any, 0)
	}

	var result []map[string]any
	if err := json.Unmarshal(bodyBytes, &result); err != nil {
		// Aligné sur test_complet.js : JSON manquant ou invalide → tableau vide
		return make([]map[string]any, 0)
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

	res, err := client.Do(req)
	if err != nil {
		fmt.Printf("❌ Connection error: %s - API down ?\n", url)
		return nil, fmt.Errorf("failed to execute request: %w", err)
	}

	for _, sc := range res.Header.Values("Set-Cookie") {
		pair := strings.SplitN(sc, ";", 2)[0]
		cookieJars[who] = pair
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

func assertNumericId(id any, label string) {
	str := fmt.Sprintf("%v", id)
	if str == "" || strings.IndexFunc(str, func(r rune) bool { return r < '0' || r > '9' }) != -1 {
		log.Fatalf("%s doit être un identifiant numérique, reçu %v", label, id)
	}
}

func assertSortedAscByField(arr []map[string]any, field, label string) {
	if len(arr) < 2 {
		return
	}
	prevRaw := fmt.Sprintf("%v", arr[0][field])
	prevNorm := normalize(prevRaw)
	for i := 1; i < len(arr); i++ {
		curRaw := fmt.Sprintf("%v", arr[i][field])
		curNorm := normalize(curRaw)
		if prevNorm > curNorm {
			log.Fatalf("Liste %s non triée croissant par %s : %q > %q", label, field, prevRaw, curRaw)
		}
		prevNorm, prevRaw = curNorm, curRaw
	}
}

func assertAdminsFirstThenName(arr []map[string]any) {
	if len(arr) < 2 {
		return
	}
	foundNonAdmin := false
	prevName := ""
	for _, cur := range arr {
		admin, ok := cur["admin"].(bool)
		if !ok {
			log.Fatalf("Objet user sans champ admin")
		}
		name := fmt.Sprintf("%v", cur["name"])
		if foundNonAdmin && admin {
			log.Fatalf("Admins doivent précéder les non-admins")
		}
		if !admin && prevName != "" && normalize(prevName) > normalize(name) {
			log.Fatalf("Tri alphabétique ascendant incorrect")
		}
		if !admin {
			foundNonAdmin = true
		}
		prevName = name
	}
}

/* ---------- Utilitaire de normalisation ---------- */
// Supprime les diacritiques pour un tri accent-insensible
func normalize(s string) string {
	t := norm.NFD.String(s)
	var b strings.Builder
	for _, r := range t {
		if unicode.Is(unicode.Mn, r) { // Mn = Mark, Non-spacing
			continue
		}
		b.WriteRune(r)
	}
	return b.String()
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
	assertNumericId(plantID, "plantID")

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
	assertNumericId(plantID, "plantID")

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
	if items, ok := foundOrder["orderItems"].([]any); !ok || len(items) == 0 {
		log.Fatalf("Items absents dans la commande")
	} else {
		first := items[0].(map[string]any)
		plant := first["plant"].(map[string]any)
		assertEq(plant, "name", plantData["name"])
	}
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
	assertSortedAscByField(plantes, "name", "plantes")

	d := map[string]any{"name": fmt.Sprintf("Plante_admin_de_test_%s", maintenant), "price": 99, "stock": 12}
	createdPlant := hit("POST", "/admin/plants", 201, d, who)
	id := int(createdPlant["id"].(float64))
	hit("PATCH", `/admin/plants/`+fmt.Sprintf("%d", id), 200, map[string]int{"price": 123}, who)
	hit("DELETE", `/admin/plants/`+fmt.Sprintf("%d", id), 200, nil, who)
}

/**
 * Teste la gestion CRUD des administrateurs et la conformité de l’ordre dans /admin/users
 * - Crée un admin temporaire
 * - Vérifie la présence et l’ordre dans la liste
 * - Teste mise à jour nom et bascule du rôle admin
 * - Supprime l’admin temporaire
 * @who jeton d’admin utilisé pour les requêtes
 */
func testAdminUsers(who string) {
	log.Println("\n📌 TEST MODULE: ADMIN USERS")

	// Création d’un admin temporaire
	adminTemp := map[string]any{
		"email":    fmt.Sprintf("admin_temp_%s@example.com", maintenant),
		"name":     fmt.Sprintf("Admin Temporaire %s", maintenant),
		"password": "password",
		"admin":    true,
	}
	created := hit("POST", "/users", 201, adminTemp, who)
	adminID := int(created["id"].(float64))
	log.Printf("   ↳ Admin temporaire créé: %s", adminTemp["email"])

	// Vérification : la liste est triée (admins d’abord puis noms)
	adminList := hitList("GET", "/admin/users", 200, nil, who)
	assertAdminsFirstThenName(adminList)
	log.Printf("   ↳ %d utilisateurs récupérés", len(adminList))

	// Recherche de l’admin temporaire dans la liste
	var cible map[string]any
	for _, a := range adminList {
		if a["email"] == adminTemp["email"] {
			cible = a
			break
		}
	}
	if cible == nil {
		log.Fatalf("Admin temporaire absent de la liste")
	}
	log.Printf("   ↳ Cible confirmée (%s, id=%d)", cible["email"], adminID)

	// Mise à jour du nom de l’admin
	nouveauNom := fmt.Sprintf("Admin_temp_modifié_%s", maintenant)
	hit("PATCH", fmt.Sprintf("/users/%d", adminID), 200, map[string]string{"name": nouveauNom}, who)
	updated := hit("GET", fmt.Sprintf("/users/%d", adminID), 200, nil, who)
	assertEq(updated, "name", nouveauNom)

	// Retrait du rôle admin
	hit("PATCH", fmt.Sprintf("/users/%d", adminID), 200, map[string]bool{"admin": false}, who)
	afterDemote := hit("GET", fmt.Sprintf("/users/%d", adminID), 200, nil, who)
	assertEq(afterDemote, "admin", false)

	// Restauration du rôle admin
	hit("PATCH", fmt.Sprintf("/users/%d", adminID), 200, map[string]bool{"admin": true}, who)
	afterPromote := hit("GET", fmt.Sprintf("/users/%d", adminID), 200, nil, who)
	assertEq(afterPromote, "admin", true)

	// Suppression de l’admin temporaire
	hit("DELETE", fmt.Sprintf("/users/%d", adminID), 200, nil, who)
	log.Printf("   ↳ Admin temporaire supprimé (%s)", adminTemp["email"])
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
