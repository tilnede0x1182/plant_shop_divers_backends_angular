package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"
	"strings"

	"plant_shop_go/internal/db"
	"plant_shop_go/internal/models"

	"golang.org/x/crypto/bcrypt"
)

/*
GET /api/admin/users
*/
func AdminListUsers(w http.ResponseWriter, r *http.Request) {
	var list []models.User
	db.Connect().Select("id", "email", "name", "admin").Find(&list)
	json.NewEncoder(w).Encode(map[string]any{"data": list})
}

/*
POST /api/users (admin) – création d’un user
*/
func AdminCreateUser(w http.ResponseWriter, r *http.Request) {
	var in struct {
		Email    string `json:"email"`
		Name     string `json:"name"`
		Password string `json:"password"`
	}
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		http.Error(w, "bad request", 400)
		return
	}
	hash, _ := bcrypt.GenerateFromPassword([]byte(in.Password), 10)
	u := models.User{Email: in.Email, Name: in.Name, Password: string(hash)}
	db.Connect().Create(&u)
	w.WriteHeader(http.StatusCreated)
	_ = json.NewEncoder(w).Encode(map[string]any{"id": u.ID})
}

/*
PATCH /api/admin/users/{id}
*/
func AdminUpdateUser(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(r.URL.Path, "/")
	id, _ := strconv.Atoi(parts[len(parts)-1])
	var in map[string]any
	json.NewDecoder(r.Body).Decode(&in)
	d := db.Connect()
	d.Model(&models.User{}).Where("id = ?", id).Updates(in)
	var u models.User
	d.First(&u, id)
	json.NewEncoder(w).Encode(u)
}

/*
DELETE /api/admin/users/{id}
*/
func AdminDeleteUser(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(r.URL.Path, "/")
	id, _ := strconv.Atoi(parts[len(parts)-1])
	db.Connect().Delete(&models.User{}, id)
	w.WriteHeader(http.StatusOK)
}
