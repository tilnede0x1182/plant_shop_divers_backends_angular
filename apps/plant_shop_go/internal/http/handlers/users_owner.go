package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"
	"plant_shop_go/internal/db"
	"plant_shop_go/internal/models"
	"plant_shop_go/internal/security"
)

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

// parseUserID extrait l ID utilisateur depuis le chemin.
func parseUserID(r *http.Request) int {
	vars := mux.Vars(r)
	id, _ := strconv.Atoi(vars["id"])
	return id
}

// sanitizeUserInput retire le champ admin si non autorise.
func sanitizeUserInput(in map[string]any, isAdmin bool) {
	if !isAdmin {
		delete(in, "admin")
	}
}

// fetchAndClearPassword charge un user et masque le mot de passe.
func fetchAndClearPassword(id int) (models.User, error) {
	var user models.User
	err := db.Connect().First(&user, id).Error
	user.Password = ""
	return user, err
}

// ==============================================================================
// Handlers
// ==============================================================================

// GetUser retourne un utilisateur par son ID.
func GetUser(w http.ResponseWriter, r *http.Request) {
	id := parseUserID(r)
	user, err := fetchAndClearPassword(id)
	if err != nil {
		http.Error(w, "user not found", http.StatusNotFound)
		return
	}
	json.NewEncoder(w).Encode(user)
}

// UpdateUser met a jour un utilisateur (proprietaire ou admin).
func UpdateUser(w http.ResponseWriter, r *http.Request) {
	claims, ok := r.Context().Value("claims").(*security.Claims)
	if !ok {
		http.Error(w, "claims not found", http.StatusInternalServerError)
		return
	}
	id := parseUserID(r)
	var in map[string]any
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		http.Error(w, "bad request", http.StatusBadRequest)
		return
	}
	sanitizeUserInput(in, claims.Admin)
	conn := db.Connect()
	if err := conn.Model(&models.User{}).Where("id = ?", id).Updates(in).Error; err != nil {
		http.Error(w, "update failed", http.StatusInternalServerError)
		return
	}
	user, _ := fetchAndClearPassword(id)
	json.NewEncoder(w).Encode(user)
}
