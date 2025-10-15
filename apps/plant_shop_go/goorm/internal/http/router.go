package httpserver

import (
	"net/http"

	"goorm/internal/http/handlers"
	"goorm/internal/http/middleware"
)

/*
Construit le routeur http standard lib, style Nest:
- /auth (public)
- /users, /orders (protégés)
- /plants (public)
*/
func NewRouter() http.Handler {
	mux := http.NewServeMux()

	// Auth
	mux.HandleFunc("POST /api/auth/register", handlers.Register)
	mux.HandleFunc("POST /api/auth/login", handlers.Login)
	mux.HandleFunc("POST /api/auth/logout", handlers.Logout)

	// Public plants
	mux.HandleFunc("GET /api/plants", handlers.Plants)
	mux.HandleFunc("GET /api/plants/", handlers.Plants)

	// Protected group
	protected := http.NewServeMux()
	protected.HandleFunc("GET /api/users/me", handlers.Me)
	protected.HandleFunc("POST /api/orders", handlers.CreateOrder)

	return withAuth(protected, mux)
}

func withAuth(protected http.Handler, mux *http.ServeMux) http.Handler {
	return http.HandlerFunc(func(response http.ResponseWriter, request *http.Request) {
		if request.URL.Path == "/users/me" || request.URL.Path == "/orders" {
			middleware.AuthGuard(protected).ServeHTTP(response, request)
			return
		}
		mux.ServeHTTP(response, request)
	})
}
