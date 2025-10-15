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
func Register(w http.ResponseWriter, r *http.Request) {
	type inp struct{ Email, Password string }
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
	user := models.User{Email: in.Email, Password: string(hash)}
	d.Create(&user)
	token, _ := security.GenerateToken(fmt.Sprint(user.ID), 24*time.Hour)
	security.SetCookie(w, token)
	w.WriteHeader(http.StatusOK)
}

/*
POST /api/auth/login
Valide credentials, renvoie cookie JWT.
*/
func Login(w http.ResponseWriter, r *http.Request) {
	type inp struct{ Email, Password string }
	var in inp
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		http.Error(w, "bad request", http.StatusBadRequest)
		return
	}
	d := db.Connect()
	var user models.User
	if err := d.Where("email = ?", in.Email).First(&user).Error; err != nil {
		http.Error(w, "invalid creds", http.StatusUnauthorized)
		return
	}
	if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(in.Password)); err != nil {
		http.Error(w, "invalid creds", http.StatusUnauthorized)
		return
	}
	token, _ := security.GenerateToken(fmt.Sprint(user.ID), 24*time.Hour)
	security.SetCookie(w, token)
	w.WriteHeader(http.StatusOK)
}

/*
GET /api/auth/me
Retourne {id,email,name,admin} du token.
*/
func Me(w http.ResponseWriter, r *http.Request) {
	claims := r.Context().Value("claims").(*security.Claims)
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
func Logout(w http.ResponseWriter, r *http.Request) {
	security.ClearCookie(w)
	w.WriteHeader(http.StatusNoContent)
}
