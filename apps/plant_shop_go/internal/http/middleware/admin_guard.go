package middleware

import (
	"net/http"
)

// AdminGuard protège une route en ne laissant passer que les admins.
// next sert les requêtes validées.
func AdminGuard(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// claims injectés par AuthGuard dans le context
		claims, ok := r.Context().Value("claims").(*Claims)
		if !ok || !claims.Admin {
			http.Error(w, "forbidden", http.StatusForbidden)
			return
		}
		next.ServeHTTP(w, r)
	})
}
