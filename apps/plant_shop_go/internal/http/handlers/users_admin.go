package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"
	"plant_shop_go/internal/db"
	"plant_shop_go/internal/models"

	"golang.org/x/crypto/bcrypt"
)

/*
GET /api/admin/users ou /api/users (pour admin)
*/
func AdminListUsers(w http.ResponseWriter, r *http.Request) {
	var list []models.User
	// On sélectionne les champs publics pour ne pas exposer le hash du mot de passe
	db.Connect().Select("id", "created_at", "updated_at", "email", "name", "admin").Find(&list)

	// *** LA CORRECTION EST ICI ***
	// Garantit un tableau JSON vide `[]` au lieu de `null` si aucun utilisateur n'est trouvé.
	if list == nil {
		list = make([]models.User, 0)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(list)
}

/*
POST /api/users (admin) – création d’un user
*/
func AdminCreateUser(w http.ResponseWriter, r *http.Request) {
	var in struct {
		Email    string `json:"email"`
		Name     string `json:"name"`
		Password string `json:"password"`
		Admin    bool   `json:"admin"`
	}
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		http.Error(w, "bad request", 400)
		return
	}
	hash, _ := bcrypt.GenerateFromPassword([]byte(in.Password), 10)
	u := models.User{Email: in.Email, Name: in.Name, Password: string(hash), Admin: in.Admin}

	if err := db.Connect().Create(&u).Error; err != nil {
		http.Error(w, "failed to create user", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	u.Password = "" // Ne jamais renvoyer le mot de passe
	json.NewEncoder(w).Encode(u)
}

/*
PATCH /api/admin/users/{id}
*/
func AdminUpdateUser(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, _ := strconv.Atoi(vars["id"])

	var in map[string]any
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		http.Error(w, "invalid json", http.StatusBadRequest)
		return
	}

	d := db.Connect()
	if err := d.Model(&models.User{}).Where("id = ?", id).Updates(in).Error; err != nil {
		http.Error(w, "update failed", http.StatusInternalServerError)
		return
	}

	var u models.User
	d.First(&u, id)
	u.Password = ""
	json.NewEncoder(w).Encode(u)
}

/*
DELETE /api/admin/users/{id}
*/
func AdminDeleteUser(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, _ := strconv.Atoi(vars["id"])
	db.Connect().Delete(&models.User{}, id)
	w.WriteHeader(http.StatusOK)
}
