package middleware

// ==============================================================================
// Importations
// ==============================================================================

import (
	"net/http"
	"plant_shop_go/internal/security"
)

// ==============================================================================
// Middleware admin
// ==============================================================================

// AdminGuard protege une route en ne laissant passer que les admins.
//
// @param next http.Handler Handler suivant dans la chaine
// @return http.Handler Handler avec verification admin
func AdminGuard(nextHandler http.Handler) http.Handler {
	return http.HandlerFunc(func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		userClaims, claimsFound := httpRequest.Context().Value("claims").(*security.Claims)
		if !claimsFound || !userClaims.Admin {
			http.Error(responseWriter, "forbidden", http.StatusForbidden)
			return
		}
		nextHandler.ServeHTTP(responseWriter, httpRequest)
	})
}
