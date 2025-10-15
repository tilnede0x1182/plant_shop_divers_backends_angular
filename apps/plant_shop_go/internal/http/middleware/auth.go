package middleware

import (
	"net/http"

	"goorm/internal/security"
)

/*
Middleware d’authentification par cookie httpOnly "ps_token"
@next handler suivant
*/
func AuthGuard(next http.Handler) http.Handler {
	return http.HandlerFunc(func(response http.ResponseWriter, request *http.Request) {
		cookie, err := request.Cookie("ps_token")
		if err != nil || cookie.Value == "" {
			http.Error(response, "unauthorized", http.StatusUnauthorized)
			return
		}
		_, err = security.ParseToken(cookie.Value)
		if err != nil {
			http.Error(response, "unauthorized", http.StatusUnauthorized)
			return
		}
		next.ServeHTTP(response, request)
	})
}
