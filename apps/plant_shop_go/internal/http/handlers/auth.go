package handlers

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"plant_shop_go/internal/db"
	"plant_shop_go/internal/models"
	"plant_shop_go/internal/security"

	"golang.org/x/crypto/bcrypt"
)

// authInput structure pour login/register.
type authInput struct {
	Email    string `json:"email"`
	Password string `json:"password"`
	Name     string `json:"name"`
}

// decodeAuthInput decode le JSON d'entree auth.
//
// @param r *http.Request Requete HTTP
// @return *authInput Input decode ou nil
// @return error Erreur de decodage
func decodeAuthInput(r *http.Request) (*authInput, error) {
	var in authInput
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		return nil, err
	}
	return &in, nil
}

// emailExists verifie si un email existe deja.
//
// @param email string Email a verifier
// @return bool True si existe
func emailExists(email string) bool {
	var exists models.User
	return db.Connect().Where("email = ?", email).First(&exists).Error == nil
}

// createUserFromInput cree un user a partir de l'input.
//
// @param in *authInput Input decode
// @return *models.User User cree
func createUserFromInput(in *authInput) *models.User {
	hash, _ := bcrypt.GenerateFromPassword([]byte(in.Password), bcrypt.DefaultCost)
	user := models.User{Email: in.Email, Password: string(hash), Name: in.Name, Admin: false}
	db.Connect().Create(&user)
	return &user
}

// setAuthCookie genere et pose le cookie JWT.
//
// @param w http.ResponseWriter Writer HTTP
// @param userID uint ID utilisateur
// @param admin bool Est admin
func setAuthCookie(w http.ResponseWriter, userID uint, admin bool) {
	token, _ := security.GenerateToken(fmt.Sprint(userID), admin, 24*time.Hour)
	security.SetCookie(w, token)
}

// Register cree un nouvel utilisateur et retourne un cookie JWT.
//
// @param w http.ResponseWriter Writer de reponse HTTP
// @param r *http.Request Requete HTTP entrante
func Register(w http.ResponseWriter, r *http.Request) {
	in, err := decodeAuthInput(r)
	if err != nil {
		http.Error(w, "bad request", http.StatusBadRequest)
		return
	}
	if emailExists(in.Email) {
		http.Error(w, "email exists", http.StatusConflict)
		return
	}
	user := createUserFromInput(in)
	setAuthCookie(w, user.ID, user.Admin)
	w.WriteHeader(http.StatusCreated)
}

// findUserByEmail cherche un user par email.
//
// @param email string Email a chercher
// @return *models.User User trouve ou nil
func findUserByEmail(email string) *models.User {
	var user models.User
	if err := db.Connect().Where("email = ?", email).First(&user).Error; err != nil {
		return nil
	}
	return &user
}

// checkPassword verifie le mot de passe.
//
// @param hash string Hash bcrypt
// @param password string Mot de passe en clair
// @return bool True si valide
func checkPassword(hash string, password string) bool {
	return bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)) == nil
}

// Login verifie les credentials et retourne un cookie JWT.
//
// @param w http.ResponseWriter Writer de reponse HTTP
// @param r *http.Request Requete HTTP entrante
func Login(w http.ResponseWriter, r *http.Request) {
	in, err := decodeAuthInput(r)
	if err != nil {
		http.Error(w, "bad request", http.StatusBadRequest)
		return
	}
	user := findUserByEmail(in.Email)
	if user == nil || !checkPassword(user.Password, in.Password) {
		http.Error(w, "invalid creds", http.StatusUnauthorized)
		return
	}
	setAuthCookie(w, user.ID, user.Admin)
	w.WriteHeader(http.StatusCreated)
}

// Me retourne les informations de l utilisateur connecte.
//
// @param w http.ResponseWriter Writer de reponse HTTP
// @param r *http.Request Requete HTTP entrante
func Me(w http.ResponseWriter, r *http.Request) {
	claims, ok := r.Context().Value("claims").(*security.Claims)
	if !ok {
		http.Error(w, "claims not found in context", http.StatusInternalServerError)
		return
	}
	var user models.User
	db.Connect().First(&user, claims.UserID)
	json.NewEncoder(w).Encode(map[string]any{
		"id": user.ID, "email": user.Email, "name": user.Name, "admin": user.Admin,
	})
}

// Logout supprime le cookie d authentification.
//
// @param w http.ResponseWriter Writer de reponse HTTP
// @param r *http.Request Requete HTTP entrante
func Logout(w http.ResponseWriter, r *http.Request) {
	security.ClearCookie(w)
	w.WriteHeader(http.StatusNoContent)
}
