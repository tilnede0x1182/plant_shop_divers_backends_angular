package handlers

import (
	"encoding/json"
	"net/http"

	"goorm/internal/security"
)

/*
GET /users/me
Retourne l’utilisateur courant depuis le JWT.
*/
func Me(response http.ResponseWriter, request *http.Request) {
	cookie, err := request.Cookie("ps_token")
	if err != nil {
		http.Error(response, "unauthorized", http.StatusUnauthorized)
		return
	}
	claims, err := security.ParseToken(cookie.Value)
	if err != nil {
		http.Error(response, "unauthorized", http.StatusUnauthorized)
		return
	}
	json.NewEncoder(response).Encode(map[string]any{
		"id":    claims.UserID,
		"email": "placeholder@example.com",
	})
}
