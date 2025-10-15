package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"
	"strings"

	"internal/db"
	"internal/models"
	"internal/security"
)

/*
GET /api/users/{id}
*/
func GetUser(w http.ResponseWriter, r *http.Request) {
	claims := r.Context().Value("claims").(*security.Claims)
	parts := strings.Split(r.URL.Path, "/")
	idParam := parts[3]
	if idParam != claims.UserID && !claims.Admin {
		http.Error(w, "forbidden", http.StatusForbidden)
		return
	}
	id, _ := strconv.Atoi(idParam)
	var u models.User
	db.Connect().First(&u, id)
	json.NewEncoder(w).Encode(u)
}

/*
PATCH /api/users/{id}
*/
func UpdateUser(w http.ResponseWriter, r *http.Request) {
	claims := r.Context().Value("claims").(*security.Claims)
	parts := strings.Split(r.URL.Path, "/")
	idParam := parts[3]
	if idParam != claims.UserID && !claims.Admin {
		http.Error(w, "forbidden", http.StatusForbidden)
		return
	}
	id, _ := strconv.Atoi(idParam)
	var in map[string]any
	json.NewDecoder(r.Body).Decode(&in)
	db.Connect().Model(&models.User{}).Where("id = ?", id).Updates(in)
	var u models.User
	db.Connect().First(&u, id)
	json.NewEncoder(w).Encode(u)
}
