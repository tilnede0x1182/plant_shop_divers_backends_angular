package middleware

// ==============================================================================
// Importations
// ==============================================================================

import (
	"fmt"
	"net/http"
	"strings"

	"plant_shop_go/internal/security"
)

// ==============================================================================
// Middleware owner
// ==============================================================================

// OwnerGuard protège /users/:id en acceptant le propriétaire ou l’admin.
func OwnerGuard(nextHandler http.Handler) http.Handler {
	return http.HandlerFunc(func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		userClaims, claimsFound := httpRequest.Context().Value("claims").(*security.Claims)
		if !claimsFound {
			http.Error(responseWriter, "unauthorized", http.StatusUnauthorized)
			return
		}
		// extraire id param de l’URL (/api/users/{id})
		pathParts := strings.Split(httpRequest.URL.Path, "/")
		if len(pathParts) < 4 || (fmt.Sprint(userClaims.UserID) != pathParts[3] && !userClaims.Admin) {
			http.Error(responseWriter, "forbidden", http.StatusForbidden)
			return
		}
		nextHandler.ServeHTTP(responseWriter, httpRequest)
	})
}
