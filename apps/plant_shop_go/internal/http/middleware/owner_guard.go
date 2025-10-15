package middleware

import (
	"fmt"
	"net/http"
	"strings"

	"plant_shop_go/internal/security"
)

// OwnerGuard protège /users/:id en acceptant le propriétaire ou l’admin.
func OwnerGuard(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		claims, ok := r.Context().Value("claims").(*security.Claims)
		if !ok {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
		// extraire id param de l’URL (/api/users/{id})
		parts := strings.Split(r.URL.Path, "/")
		if len(parts) < 4 || (fmt.Sprint(claims.UserID) != parts[3] && !claims.Admin) {
			http.Error(w, "forbidden", http.StatusForbidden)
			return
		}
		next.ServeHTTP(w, r)
	})
}
