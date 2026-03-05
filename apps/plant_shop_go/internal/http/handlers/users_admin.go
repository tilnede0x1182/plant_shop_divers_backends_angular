package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"
	"plant_shop_go/internal/db"
	"plant_shop_go/internal/models"

	"golang.org/x/crypto/bcrypt"
	"strings"
)

// adminUserInput structure pour creation/update user admin.
type adminUserInput struct {
	Email    string `json:"email"`
	Name     string `json:"name"`
	Password string `json:"password"`
	Admin    bool   `json:"admin"`
}

// decodeAdminUserInput decode le JSON d'entree pour user admin.
//
// @param r *http.Request Requete HTTP entrante
// @return *adminUserInput Input decode ou nil si erreur
// @return error Erreur de decodage
func decodeAdminUserInput(r *http.Request) (*adminUserInput, error) {
	var in adminUserInput
	err := json.NewDecoder(r.Body).Decode(&in)
	if err != nil {
		return nil, err
	}
	return &in, nil
}

// hashPassword genere le hash bcrypt du mot de passe.
//
// @param password string Mot de passe en clair
// @return string Hash bcrypt
func hashPassword(password string) string {
	hash, _ := bcrypt.GenerateFromPassword([]byte(password), 10)
	return string(hash)
}

// handleDuplicateEmail gere l'erreur de duplication email.
//
// @param w http.ResponseWriter Writer HTTP
// @param err error Erreur a analyser
func handleDuplicateEmail(w http.ResponseWriter, err error) {
	if strings.Contains(err.Error(), "duplicate key") {
		http.Error(w, "email exists", http.StatusConflict)
	} else {
		http.Error(w, "failed to create user", http.StatusInternalServerError)
	}
}

// sendUserResponse envoie la reponse JSON user sans mot de passe.
//
// @param w http.ResponseWriter Writer HTTP
// @param u *models.User Utilisateur a envoyer
// @param status int Code HTTP
func sendUserResponse(w http.ResponseWriter, u *models.User, status int) {
	if status != 0 {
		w.WriteHeader(status)
	}
	u.Password = ""
	json.NewEncoder(w).Encode(u)
}

// AdminListUsers liste tous les utilisateurs (route admin).
//
// @param w http.ResponseWriter Writer de reponse HTTP
// @param r *http.Request Requete HTTP entrante
func AdminListUsers(w http.ResponseWriter, r *http.Request) {
	var list []models.User
	db.Connect().Select("id", "created_at", "updated_at", "email", "name", "admin").Order("admin DESC, name ASC").Find(&list)
	if list == nil {
		list = make([]models.User, 0)
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(list)
}

// AdminCreateUser cree un utilisateur (route admin).
//
// @param w http.ResponseWriter Writer de reponse HTTP
// @param r *http.Request Requete HTTP entrante
func AdminCreateUser(w http.ResponseWriter, r *http.Request) {
	in, err := decodeAdminUserInput(r)
	if err != nil {
		http.Error(w, "bad request", 400)
		return
	}
	u := models.User{Email: in.Email, Name: in.Name, Password: hashPassword(in.Password), Admin: in.Admin}
	if err := db.Connect().Create(&u).Error; err != nil {
		handleDuplicateEmail(w, err)
		return
	}
	sendUserResponse(w, &u, http.StatusCreated)
}

// AdminUpdateUser met a jour un utilisateur (route admin).
//
// @param w http.ResponseWriter Writer de reponse HTTP
// @param r *http.Request Requete HTTP entrante
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
	sendUserResponse(w, &u, 0)
}

// AdminDeleteUser supprime un utilisateur (route admin).
//
// @param w http.ResponseWriter Writer de reponse HTTP
// @param r *http.Request Requete HTTP entrante
func AdminDeleteUser(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, _ := strconv.Atoi(vars["id"])
	db.Connect().Unscoped().Delete(&models.User{}, id)
	w.WriteHeader(http.StatusOK)
}
