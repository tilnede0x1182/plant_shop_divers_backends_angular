// # Importations
package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

/* ---------- variables globales ---------- */
var cookieJars = map[string]string{
	"admin": "",
	"user":  "",
}
var maintenant = time.Now().Format("20060102150405")

/* ---------- configuration ---------- */
var config = struct {
	apiBase  string
	logLevel string
	adminE   string
	adminP   string
}{
	apiBase:  getenv("API_BASE_URL", "http://localhost:4100/api"),
	logLevel: "verbose", // silent | normal | verbose
	adminE:   getenv("ADMIN_EMAIL", "admin1@planteshop.com"),
	adminP:   getenv("ADMIN_PASSWORD", "password"),
}

/* ---------- utilitaires ---------- */
func getenv(k, def string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return def
}
func logf(fmtStr string, v ...any) {
	if config.logLevel != "silent" {
		log.Printf(fmtStr, v...)
	}
}
func hit(method, route string, expect int, body any, who string) map[string]any {
	url := config.apiBase + route
	label := method + " " + route

	var rdr *bytes.Reader
	if body != nil {
		b, _ := json.Marshal(body)
		rdr = bytes.NewReader(b)
	} else {
		rdr = bytes.NewReader(nil)
	}

	req, _ := http.NewRequest(method, url, rdr)
	req.Header.Set("Content-Type", "application/json")
	if ck := cookieJars[who]; ck != "" {
		req.Header.Set("Cookie", ck)
	}

	res, err := http.DefaultClient.Do(req)
	if err != nil {
		log.Fatalf("❌ %s: connexion impossible (%v)", label, err)
	}
	/* --- cookie éventuel --- */
	if set := res.Header.Get("Set-Cookie"); set != "" {
		cookieJars[who] = strings.Split(strings.Split(set, ",")[0], ";")[0]
	}

	ok := res.StatusCode == expect
	logf("%s %s [%d]", map[bool]string{true: "✅", false: "❌"}[ok], label, res.StatusCode)
	if !ok {
		buf := new(bytes.Buffer)
		buf.ReadFrom(res.Body)
		log.Fatalf("API %s → %d (attendu %d)\n%s", label, res.StatusCode, expect, buf.String())
	}

	var out map[string]any
	_ = json.NewDecoder(res.Body).Decode(&out)
	return out
}
func assertEq(obj map[string]any, key string, expected any) {
	actual := obj[key]
	ok := actual == expected
	logf("   %s ↳ %s=%v (attendu %v)", map[bool]string{true: "✅", false: "❌"}[ok], key, actual, expected)
	if !ok {
		log.Fatalf("Assertion échouée: %s=%v, attendu %v", key, actual, expected)
	}
}

/* ---------- helpers ---------- */
func login(email, pwd, who string) { hit("POST", "/auth/login", 201, M{"email": email, "password": pwd}, who) }
func registerUser(name, email, pwd, who string) {
	hit("POST", "/auth/register", 201, M{"name": name, "email": email, "password": pwd}, who)
}
func findUserIdByEmail(adminWho, email string) int {
	users := hit("GET", "/users", 200, nil, adminWho)["data"].([]any)
	for _, u := range users {
		m := u.(map[string]any)
		if m["email"] == email {
			return int(m["id"].(float64))
		}
	}
	log.Fatalf("User %s introuvable", email)
	return 0
}

/* ---------- alias map ---------- */
type M = map[string]any

/* ---------- modules de test ---------- */
func testPlants(who string) {
	log.Println("\n📌 TEST MODULE: PLANTS (admin)")
	p := M{"name": "Test Plant", "price": 10, "stock": 5}
	obj := hit("POST", "/admin/plants", 201, p, who)
	id := int(obj["id"].(float64))

	assertEq(hit("GET", "/plants/"+itoa(id), 200, nil, who), "name", p["name"])
	hit("PATCH", "/admin/plants/"+itoa(id), 200, M{"price": 15}, who)
	assertEq(hit("GET", "/plants/"+itoa(id), 200, nil, who), "price", 15.0)
	hit("DELETE", "/admin/plants/"+itoa(id), 200, nil, who)
}
func testUsers(who string) {
	log.Println("\n📌 TEST MODULE: USERS (admin)")
	user := M{
		"email":    "utilisateur_test_" + maintenant + "@example.com",
		"name":     "Utilisateur " + maintenant,
		"password": "pass123",
	}
	obj := hit("POST", "/users", 201, user, who)
	id := int(obj["id"].(float64))
	hit("PATCH", "/users/"+itoa(id), 200, M{"name": "Tester Update"}, who)
	assertEq(hit("GET", "/users/"+itoa(id), 200, nil, who), "name", "Tester Update")
	hit("DELETE", "/users/"+itoa(id), 200, nil, who)
}
func testOrders(adminWho, userWho string) {
	log.Println("\n📌 TEST MODULE: ORDERS & ORDER ITEMS")
	plant := M{"name": "Plante_"+maintenant, "price": 10, "stock": 5}
	pid := int(hit("POST", "/admin/plants", 201, plant, adminWho)["id"].(float64))

	order := hit("POST", "/orders", 201, M{"items": []M{{"plantId": pid, "quantity": 2}}}, userWho)
	oid := int(order["id"].(float64))

	hit("PATCH", "/orders/"+itoa(oid), 200, M{"status": "shipped"}, adminWho)
	cmds := hit("GET", "/orders", 200, nil, userWho)["data"].([]any)
	found := false
	for _, o := range cmds {
		m := o.(map[string]any)
		if int(m["id"].(float64)) == oid {
			assertEq(m, "status", "shipped")
			found = true
		}
	}
	if !found {
		log.Fatalf("Commande %d introuvable", oid)
	}
	hit("DELETE", "/orders/"+itoa(oid), 200, nil, adminWho)
	hit("DELETE", "/admin/plants/"+itoa(pid), 200, nil, adminWho)
}
func testUserProfile(adminWho, userWho, email string) {
	log.Println("\n📌 TEST MODULE: USER PROFILE (user)")
	uid := findUserIdByEmail(adminWho, email)

	assertEq(hit("GET", "/users/"+itoa(uid), 200, nil, userWho), "id", float64(uid))
	newName := "Utilisateur_" + maintenant
	hit("PATCH", "/users/"+itoa(uid), 200, M{"name": newName}, userWho)
	assertEq(hit("GET", "/users/"+itoa(uid), 200, nil, userWho), "name", newName)

	hit("PATCH", "/users/"+itoa(uid), 200, M{"admin": true}, userWho)
	assertEq(hit("GET", "/users/"+itoa(uid), 200, nil, adminWho), "admin", false)
}
func testAuthRoles(adminWho, userWho string) {
	log.Println("\n📌 TEST MODULE: ROLES")
	hit("POST", "/admin/plants", 403, M{"name": "Bad", "price": 1, "stock": 1}, userWho)
	pid := int(hit("POST", "/admin/plants", 201, M{"name": "Good", "price": 1, "stock": 1}, adminWho)["id"].(float64))
	hit("DELETE", "/admin/plants/"+itoa(pid), 200, nil, adminWho)
	hit("GET", "/users", 403, nil, userWho)
}
func testAdminPlants(who string) {
	log.Println("\n📌 TEST MODULE: ADMIN PLANTS")
	hit("GET", "/admin/plants", 200, nil, who)
	p := M{"name": "Plante_admin_" + maintenant, "price": 99, "stock": 12}
	id := int(hit("POST", "/admin/plants", 201, p, who)["id"].(float64))
	hit("PATCH", "/admin/plants/"+itoa(id), 200, M{"price": 123}, who)
	hit("DELETE", "/admin/plants/"+itoa(id), 200, nil, who)
}
func testAdminUsers(who string) {
	log.Println("\n📌 TEST MODULE: ADMIN USERS")
	usrs := hit("GET", "/admin/users", 200, nil, who)["data"].([]any)
	if len(usrs) == 0 {
		log.Fatalf("Pas d'utilisateurs admin trouvés")
	}
	u := usrs[0].(map[string]any)
	newName := "Admin_mod_" + maintenant
	uid := int(u["id"].(float64))
	hit("PATCH", "/admin/users/"+itoa(uid), 200, M{"name": newName}, who)
	assertEq(hit("GET", "/users/"+itoa(uid), 200, nil, who), "name", newName)
}
func testAuthMe(who string) {
	log.Println("\n📌 TEST MODULE: AUTH /me")
	me := hit("GET", "/auth/me", 200, nil, who)
	if me["email"] == nil {
		log.Fatalf("Réponse invalide pour /auth/me")
	}
	logf("   ↳ Utilisateur connecté: %s (%s)", me["email"], me["name"])
}

/* ---------- exécution ---------- */
func main() {
	fmt.Printf("🧪 Démarrage des tests: %s\n", config.apiBase)

	login(config.adminE, config.adminP, "admin")
	userEmail := "utilisateur_"+maintenant+"@example.com"
	registerUser("User", userEmail, "pass123", "user")

	testPlants("admin")
	testUsers("admin")
	testOrders("admin", "user")
	testUserProfile("admin", "user", userEmail)
	testAuthRoles("admin", "user")
	testAdminPlants("admin")
	testAdminUsers("admin")
	testAuthMe("user")

	fmt.Println("\n🎉 Tous les tests ont réussi!")
	os.Exit(0)
}

/* ---------- helpers mineurs ---------- */
func itoa(i int) string { return strconv.Itoa(i) }
