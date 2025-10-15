package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"
	"strings"

	"plant_shop_go/internal/db"
	"plant_shop_go/internal/models"
)

/*
GET /api/admin/users
*/
func AdminListUsers(w http.ResponseWriter, r *http.Request) {
	var list []models.User
	db.Connect().Select("id", "email", "name", "admin").Find(&list)
	json.NewEncoder(w).Encode(list)
}

/*
PATCH /api/admin/users/{id}
*/
func AdminUpdateUser(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(r.URL.Path, "/")
	id, _ := strconv.Atoi(parts[4])
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
	id, _ := strconv.Atoi(parts[4])
	db.Connect().Delete(&models.User{}, id)
	w.WriteHeader(http.StatusNoContent)
}
