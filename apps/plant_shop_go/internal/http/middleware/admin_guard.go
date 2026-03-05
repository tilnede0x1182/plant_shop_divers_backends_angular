package middleware

import (
	"net/http"
	"plant_shop_go/internal/security"
)

// AdminGuard protege une route en ne laissant passer que les admins.
//
// @param next http.Handler Handler suivant dans la chaine
// @return http.Handler Handler avec verification admin
func AdminGuard(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// claims injectés par AuthGuard dans le context
		claims, ok := r.Context().Value("claims").(*security.Claims)
		if !ok || !claims.Admin {
			http.Error(w, "forbidden", http.StatusForbidden)
			return
		}
		next.ServeHTTP(w, r)
	})
}
