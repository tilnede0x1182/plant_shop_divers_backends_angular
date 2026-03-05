package middleware

// ==============================================================================
// Importations
// ==============================================================================

import (
	"context"
	"net/http"

	"plant_shop_go/internal/security"
)

// ==============================================================================
// Middleware d authentification
// ==============================================================================

// AuthGuard protege une route en verifiant le cookie JWT.
// Injecte les claims dans le contexte de la requete.
//
// @param next http.Handler Handler suivant dans la chaine
// @return http.Handler Handler avec verification JWT
func AuthGuard(nextHandler http.Handler) http.Handler {
	return http.HandlerFunc(func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		authCookie, cookieError := httpRequest.Cookie("ps_token")
		if cookieError != nil || authCookie.Value == "" {
			http.Error(responseWriter, "unauthorized", http.StatusUnauthorized)
			return
		}
		userClaims, parseError := security.ParseToken(authCookie.Value)
		if parseError != nil {
			http.Error(responseWriter, "unauthorized", http.StatusUnauthorized)
			return
		}
		requestContext := context.WithValue(httpRequest.Context(), "claims", userClaims)
		nextHandler.ServeHTTP(responseWriter, httpRequest.WithContext(requestContext))
	})
}
