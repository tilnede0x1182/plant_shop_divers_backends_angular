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

/*
POST /api/auth/register
Crée un user, renvoie 201 + cookie JWT.
*/
// Register crée un nouvel utilisateur et retourne un cookie JWT.
func Register(w http.ResponseWriter, r *http.Request) {
	type inp struct {
		Email    string `json:"email"`
		Password string `json:"password"`
		Name     string `json:"name"`
	}
	var in inp
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		http.Error(w, "bad request", http.StatusBadRequest)
		return
	}
	d := db.Connect()
	// conflit si email exists
	var exists models.User
	if err := d.Where("email = ?", in.Email).First(&exists).Error; err == nil {
		http.Error(w, "email exists", http.StatusConflict)
		return
	}
	hash, _ := bcrypt.GenerateFromPassword([]byte(in.Password), bcrypt.DefaultCost)
	user := models.User{Email: in.Email, Password: string(hash), Name: in.Name, Admin: false}
	d.Create(&user)
	// fmt.Printf("[DEBUG] Utilisateur créé : email=%s id=%d\n", user.Email, user.ID)
	token, _ := security.GenerateToken(fmt.Sprint(user.ID), user.Admin, 24*time.Hour)

	security.SetCookie(w, token)
	w.WriteHeader(http.StatusCreated)
}

/*
POST /api/auth/login
Valide credentials, renvoie 201 + cookie JWT.
*/
// Login vérifie les credentials et retourne un cookie JWT.
func Login(w http.ResponseWriter, r *http.Request) {
	type inp struct{ Email, Password string }
	var in inp
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		http.Error(w, "bad request", http.StatusBadRequest)
		return
	}
	d := db.Connect()
	// fmt.Printf("[DEBUG] Tentative de login pour email=%s\n", in.Email)
	var user models.User
	if err := d.Where("email = ?", in.Email).First(&user).Error; err != nil {
		http.Error(w, "invalid creds", http.StatusUnauthorized)
		return
	}
	if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(in.Password)); err != nil {
		http.Error(w, "invalid creds", http.StatusUnauthorized)
		return
	}
	token, _ := security.GenerateToken(fmt.Sprint(user.ID), user.Admin, 24*time.Hour)
	security.SetCookie(w, token)
	w.WriteHeader(http.StatusCreated)
}

/*
GET /api/auth/me
Retourne {id,email,name,admin} du token.
*/
// Me retourne les informations de l utilisateur connecté.
func Me(w http.ResponseWriter, r *http.Request) {
	claims, ok := r.Context().Value("claims").(*security.Claims)
	if !ok {
		http.Error(w, "claims not found in context", http.StatusInternalServerError)
		return
	}

	d := db.Connect()
	var user models.User
	d.First(&user, claims.UserID)
	json.NewEncoder(w).Encode(map[string]any{
		"id":    user.ID,
		"email": user.Email,
		"name":  user.Name,
		"admin": user.Admin,
	})
}

/*
POST /api/auth/logout
Efface cookie.
*/
// Logout supprime le cookie d authentification.
func Logout(w http.ResponseWriter, r *http.Request) {
	security.ClearCookie(w)
	w.WriteHeader(http.StatusNoContent)
}
