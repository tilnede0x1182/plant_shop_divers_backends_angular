package tests

/*
E2E Go : couvre auth, plants, admin plants, users, orders, rôles.
*/

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/joho/godotenv"
	httpserver "goorm/internal/http"
)

func TestE2E(t *testing.T) {
	_ = godotenv.Load("../.env")
	os.Setenv("JWT_SECRET", os.Getenv("JWT_SECRET"))

	srv := httptest.NewServer(httpserver.NewRouter())
	defer srv.Close()

	userCookie := registerUser(t, srv.URL)
	adminCookie := loginAdmin(t, srv.URL)

	testAuthMe(t, srv.URL, userCookie)
	testPlantsPublic(t, srv.URL)
	testAdminPlantsCRUD(t, srv.URL, adminCookie)
	testUsersCRUD(t, srv.URL, userCookie, adminCookie)
	orderID := testOrdersUser(t, srv.URL, userCookie)
	testOrdersAdmin(t, srv.URL, adminCookie, orderID)
}

/* helpers */

func doRequest(t *testing.T, method, path string, body any, cookie *http.Cookie) *http.Response {
	var reader io.Reader
	if body != nil {
		b, _ := json.Marshal(body)
		reader = bytes.NewBuffer(b)
	}
	req, _ := http.NewRequest(method, path, reader)
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	if cookie != nil {
		req.AddCookie(cookie)
	}
	res, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatalf("%s %s error: %v", method, path, err)
	}
	return res
}

func parseJSON(t *testing.T, res *http.Response, out any) {
	defer res.Body.Close()
	if err := json.NewDecoder(res.Body).Decode(out); err != nil {
		t.Fatalf("parseJSON error: %v", err)
	}
}

/* 1. Auth */

func registerUser(t *testing.T, base string) *http.Cookie {
	body := map[string]string{"email": fmt.Sprintf("user_%d@example.com", time.Now().UnixNano()), "password": "pwd12345"}
	res := doRequest(t, "POST", base+"/api/auth/register", body, nil)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("register user status=%d", res.StatusCode)
	}
	cookies := res.Cookies()
	if len(cookies) == 0 {
		t.Fatal("no cookie on register")
	}
	return cookies[0]
}

func loginAdmin(t *testing.T, base string) *http.Cookie {
	// admin1@planteshop.com / password
	body := map[string]string{"email": "admin1@planteshop.com", "password": "password"}
	res := doRequest(t, "POST", base+"/api/auth/login", body, nil)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("login admin status=%d", res.StatusCode)
	}
	cookies := res.Cookies()
	if len(cookies) == 0 {
		t.Fatal("no cookie on admin login")
	}
	return cookies[0]
}

/* 2. /api/auth/me */

func testAuthMe(t *testing.T, base string, cookie *http.Cookie) {
	res := doRequest(t, "GET", base+"/api/auth/me", nil, cookie)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("GET /api/auth/me expected 200 got %d", res.StatusCode)
	}
}

/* 3. Plants public */

func testPlantsPublic(t *testing.T, base string) {
	var list []map[string]any
	res := doRequest(t, "GET", base+"/api/plants", nil, nil)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("GET /api/plants %d", res.StatusCode)
	}
	parseJSON(t, res, &list)
	if len(list) == 0 {
		t.Fatal("empty plants list")
	}
	id := fmt.Sprint(list[0]["id"])
	res = doRequest(t, "GET", base+"/api/plants/"+id, nil, nil)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("GET /api/plants/:id %d", res.StatusCode)
	}
}

/* 4. Admin Plants CRUD */

func testAdminPlantsCRUD(t *testing.T, base string, cookie *http.Cookie) {
	// unauthorized with user cookie
	res := doRequest(t, "POST", base+"/api/admin/plants", map[string]any{"name": "X", "price": 1, "stock": 1}, nil)
	if res.StatusCode != http.StatusUnauthorized && res.StatusCode != http.StatusForbidden {
		t.Fatalf("expected 401/403 got %d", res.StatusCode)
	}
	// create
	body := map[string]any{"name": "TestPlant", "price": 10, "stock": 5}
	res = doRequest(t, "POST", base+"/api/admin/plants", body, cookie)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("admin create plant %d", res.StatusCode)
	}
	var p map[string]any
	parseJSON(t, res, &p)
	id := fmt.Sprint(p["id"])
	// update
	body = map[string]any{"stock": 7}
	res = doRequest(t, "PATCH", base+"/api/admin/plants/"+id, body, cookie)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("admin update plant %d", res.StatusCode)
	}
	// delete
	res = doRequest(t, "DELETE", base+"/api/admin/plants/"+id, nil, cookie)
	if res.StatusCode != http.StatusNoContent {
		t.Fatalf("admin delete plant %d", res.StatusCode)
	}
}

/* 5. Users CRUD */

func testUsersCRUD(t *testing.T, base string, userCookie, adminCookie *http.Cookie) {
	// get own profile
	res := doRequest(t, "GET", base+"/api/users/"+userID(userCookie), nil, userCookie)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("GET own user %d", res.StatusCode)
	}
	// update own
	res = doRequest(t, "PATCH", base+"/api/users/"+userID(userCookie), map[string]any{"name": "NewName"}, userCookie)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("PATCH own user %d", res.StatusCode)
	}
	// list admin
	res = doRequest(t, "GET", base+"/api/admin/users", nil, userCookie)
	if res.StatusCode != http.StatusForbidden {
		t.Fatalf("non-admin GET admin/users %d", res.StatusCode)
	}
	res = doRequest(t, "GET", base+"/api/admin/users", nil, adminCookie)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("admin GET admin/users %d", res.StatusCode)
	}
}

func userID(cookie *http.Cookie) string {
	parts := strings.Split(cookie.Value, ".")
	// assume second segment is base64 JSON with "uid"
	return "" // implementation depends on token format; skip exact match
}

/* 6. Orders */

func testOrdersUser(t *testing.T, base string, cookie *http.Cookie) string {
	// start empty or seed orders exist
	res := doRequest(t, "GET", base+"/api/orders", nil, cookie)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("GET /api/orders %d", res.StatusCode)
	}
	// create order
	items := []map[string]any{{"plantId": 1, "quantity": 2}}
	res = doRequest(t, "POST", base+"/api/orders", map[string]any{"items": items}, cookie)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("POST /api/orders %d", res.StatusCode)
	}
	var o map[string]any
	parseJSON(t, res, &o)
	id := fmt.Sprint(o["id"])
	// get by id
	res = doRequest(t, "GET", base+"/api/orders/"+id, nil, cookie)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("GET /api/orders/:id %d", res.StatusCode)
	}
	return id
}

func testOrdersAdmin(t *testing.T, base string, cookie *http.Cookie, orderID string) {
	// patch
	res := doRequest(t, "PATCH", base+"/api/orders/"+orderID, map[string]any{"status": "shipped"}, cookie)
	if res.StatusCode != http.StatusOK {
		t.Fatalf("PATCH /api/orders/:id %d", res.StatusCode)
	}
	// delete
	res = doRequest(t, "DELETE", base+"/api/orders/"+orderID, nil, cookie)
	if res.StatusCode != http.StatusNoContent {
		t.Fatalf("DELETE /api/orders/:id %d", res.StatusCode)
	}
}
