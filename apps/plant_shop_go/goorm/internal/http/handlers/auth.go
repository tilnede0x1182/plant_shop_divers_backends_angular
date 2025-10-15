package handlers

import (
	"encoding/json"
	"net/http"
	"time"

	"goorm/internal/security"
)

/*
POST /auth/register
Inscription factice (stub). À relier au modèle User ensuite.
@response sortie JSON
@request entrée JSON
*/
func Register(response http.ResponseWriter, request *http.Request) {
	type input struct {
		Email    string `json:"email"`
		Password string `json:"password"`
	}
	var payload input
	json.NewDecoder(request.Body).Decode(&payload)

	// TODO: vérifier unicité, hachage, persistence
	token, _ := security.GenerateToken("new-user-id", 24*time.Hour)
	setCookie(response, token)
	json.NewEncoder(response).Encode(map[string]any{"status": "ok"})
}

/*
POST /auth/login
Login factice (stub). À relier au modèle User et hachage.
*/
func Login(response http.ResponseWriter, request *http.Request) {
	type input struct {
		Email    string `json:"email"`
		Password string `json:"password"`
	}
	var payload input
	json.NewDecoder(request.Body).Decode(&payload)

	// TODO: valider mot de passe, chercher user
	token, _ := security.GenerateToken("user-id", 24*time.Hour)
	setCookie(response, token)
	json.NewEncoder(response).Encode(map[string]any{"status": "ok"})
}

/*
POST /auth/logout
Supprime le cookie.
*/
func Logout(response http.ResponseWriter, request *http.Request) {
	http.SetCookie(response, &http.Cookie{
		Name:     "ps_token",
		Value:    "",
		Path:     "/",
		HttpOnly: true,
		SameSite: http.SameSiteLaxMode,
		MaxAge:   -1,
	})
	response.WriteHeader(http.StatusNoContent)
}

func setCookie(response http.ResponseWriter, token string) {
	http.SetCookie(response, &http.Cookie{
		Name:     "ps_token",
		Value:    token,
		Path:     "/",
		HttpOnly: true,
		SameSite: http.SameSiteLaxMode,
		Secure:   false,
	})
}
