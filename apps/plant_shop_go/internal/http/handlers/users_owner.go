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

/*
GET /api/users/{id}
*/
func GetUser(w http.ResponseWriter, r *http.Request) {
	// Cette fonction est déjà protégée par OwnerGuard, donc l'accès est sécurisé.
	vars := mux.Vars(r)
	id, _ := strconv.Atoi(vars["id"])

	var u models.User
	if err := db.Connect().First(&u, id).Error; err != nil {
		http.Error(w, "user not found", http.StatusNotFound)
		return
	}

	// On ne renvoie jamais le mot de passe
	u.Password = ""
	json.NewEncoder(w).Encode(u)
}

/*
PATCH /api/users/{id}
*/
func UpdateUser(w http.ResponseWriter, r *http.Request) {
	// OwnerGuard a déjà vérifié que l'utilisateur est soit le propriétaire, soit un admin.
	// Maintenant, nous devons vérifier si l'utilisateur qui fait la requête est un admin
	// pour l'autoriser à modifier le champ "admin".

	claims, ok := r.Context().Value("claims").(*security.Claims)
	if !ok {
		http.Error(w, "claims not found in context", http.StatusInternalServerError)
		return
	}

	vars := mux.Vars(r)
	id, _ := strconv.Atoi(vars["id"])

	var in map[string]any
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		http.Error(w, "bad request", http.StatusBadRequest)
		return
	}

	// *** LA CORRECTION EST ICI ***
	// Si l'utilisateur n'est PAS un admin, on supprime le champ "admin" du payload
	// pour l'empêcher de se promouvoir lui-même.
	if !claims.Admin {
		delete(in, "admin")
	}

	// Appliquer les modifications
	d := db.Connect()
	if err := d.Model(&models.User{}).Where("id = ?", id).Updates(in).Error; err != nil {
		http.Error(w, "update failed", http.StatusInternalServerError)
		return
	}

	// Renvoyer l'utilisateur mis à jour
	var u models.User
	d.First(&u, id)
	u.Password = "" // Ne jamais renvoyer le hash
	json.NewEncoder(w).Encode(u)
}
