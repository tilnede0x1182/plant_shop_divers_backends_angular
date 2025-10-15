// # Importations
package tests

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
)

// # Fonctions utilitaires
func req(t *testing.T, m, url string, body any, c *http.Cookie) *http.Response {
	var rdr *bytes.Reader
	if body != nil {
		b, _ := json.Marshal(body)
		rdr = bytes.NewReader(b)
	} else {
		rdr = bytes.NewReader(nil)
	}
	r, _ := http.NewRequest(m, url, rdr)
	if body != nil {
		r.Header.Set("Content-Type", "application/json")
	}
	if c != nil {
		r.AddCookie(c)
	}
	res, err := http.DefaultClient.Do(r)
	if err != nil {
		t.Fatalf("%s %s: %v", m, url, err)
	}
	return res
}
func readJSON(t *testing.T, res *http.Response, v any) {
	defer res.Body.Close()
	if err := json.NewDecoder(res.Body).Decode(v); err != nil {
		t.Fatalf("parse: %v", err)
	}
}

// # Fonctions principales
func TestE2E(t *testing.T) {
	runE2E(t)
}

func runE2E(t testing.TB) {
	_ = godotenv.Load("../.env")
	srv := httptest.NewServer(httpserver.NewRouter())
	defer srv.Close()

	user := register(t, srv.URL)
	admin := login(t, srv.URL, "admin1@planteshop.com", "password")
	uid := me(t, srv.URL, user)

	publicPlants(t, srv.URL)
	pid := adminPlants(t, srv.URL, admin)
	users(t, srv.URL, uid, user, admin)
	oid := ordersUser(t, srv.URL, pid, user)
	ordersAdmin(t, srv.URL, oid, admin)
}

// ## Auth
func register(t testing.TB, base string) *http.Cookie {
	data := map[string]string{"email": time.Now().Format("user_150405@ex.com"), "password": "pwd12345"}
	res := req(t, "POST", base+"/api/auth/register", data, nil)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("register %d", res.StatusCode)
	}
	return res.Cookies()[0]
}
func login(t testing.TB, b, mail, pwd string) *http.Cookie {
	res := req(t, "POST", b+"/api/auth/login", map[string]string{"email": mail, "password": pwd}, nil)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("login %d", res.StatusCode)
	}
	return res.Cookies()[0]
}
func me(t testing.TB, base string, c *http.Cookie) string {
	var out struct{ ID uint }
	res := req(t, "GET", base+"/api/auth/me", nil, c)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("/me %d", res.StatusCode)
	}
	readJSON(t, res, &out)
	return fmt.Sprint(out.ID)
}

// ## Plants
func publicPlants(t testing.TB, base string) {
	res := req(t, "GET", base+"/api/plants", nil, nil)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("plants list %d", res.StatusCode)
	}
}
func adminPlants(t testing.TB, b string, c *http.Cookie) string {
	testPlant := map[string]any{"name": "TestPlant", "price": 9, "stock": 3}
	res := req(t, "POST", b+"/api/admin/plants", testPlant, c)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("create plant %d", res.StatusCode)
	}
	var p struct{ ID uint }
	readJSON(t, res, &p)
	id := fmt.Sprint(p.ID)
	req(t, "PATCH", b+"/api/admin/plants/"+id, map[string]any{"stock": 5}, c)
	req(t, "DELETE", b+"/api/admin/plants/"+id, nil, c)
	return id
}

// ## Users
func users(t testing.TB, b, uid string, user, admin *http.Cookie) {
	req(t, "GET", b+"/api/users/"+uid, nil, user)
	req(t, "PATCH", b+"/api/users/"+uid, map[string]any{"name": "X"}, user)
	req(t, "GET", b+"/api/admin/users", nil, admin)
}

// ## Orders
func ordersUser(t testing.TB, b, pid string, c *http.Cookie) string {
	items := []map[string]any{{"plantId": pid, "quantity": 1}}
	res := req(t, "POST", b+"/api/orders", map[string]any{"items": items}, c)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("create order %d", res.StatusCode)
	}
	var o struct{ ID uint }
	readJSON(t, res, &o)
	return fmt.Sprint(o.ID)
}
func ordersAdmin(t testing.TB, b, oid string, c *http.Cookie) {
	req(t, "PATCH", b+"/api/orders/"+oid, map[string]any{"status": "shipped"}, c)
	req(t, "DELETE", b+"/api/orders/"+oid, nil, c)
}

// # Main
func main() {
	os.Exit(runMain())
}
func runMain() int {
	t := &testing.T{}
	runE2E(t)
	return 0
}
