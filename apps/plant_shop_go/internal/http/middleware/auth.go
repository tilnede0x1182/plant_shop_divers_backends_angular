package middleware

import (
	"context"
	"net/http"

	"plant_shop_go/internal/security"
)

// AuthGuard protege une route en verifiant le cookie JWT.
// Injecte les claims dans le contexte de la requete.
//
// @param next http.Handler Handler suivant dans la chaine
// @return http.Handler Handler avec verification JWT
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
