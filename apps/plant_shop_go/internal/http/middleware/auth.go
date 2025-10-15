package middleware

import (
	"context"
	"net/http"

	"plant_shop_go/internal/security"
)

/*
Middleware d’authentification par cookie httpOnly "ps_token"
Injecte les claims dans le contexte de la requête sous la clé "claims".
*/
func AuthGuard(next http.Handler) http.Handler {
	return http.HandlerFunc(func(response http.ResponseWriter, request *http.Request) {
		cookie, err := request.Cookie("ps_token")
		if err != nil || cookie.Value == "" {
			http.Error(response, "unauthorized", http.StatusUnauthorized)
			return
		}
		claims, err := security.ParseToken(cookie.Value)
		if err != nil {
			http.Error(response, "unauthorized", http.StatusUnauthorized)
			return
		}
		// injecter les claims dans le contexte pour que AdminGuard / OwnerGuard y accèdent
		ctx := context.WithValue(request.Context(), "claims", claims)
		next.ServeHTTP(response, request.WithContext(ctx))
	})
}
