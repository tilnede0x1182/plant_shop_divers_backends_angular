package httpserver

import (
	"net/http"
	"strings"

	"goorm/internal/http/handlers"
	"goorm/internal/http/middleware"
)

func NewRouter() http.Handler {
	mux := http.NewServeMux()

	// Auth (public + me)
	mux.HandleFunc("/api/auth/register", handlers.Register)
	mux.HandleFunc("/api/auth/login",    handlers.Login)
	mux.Handle("/api/auth/logout",       middleware.AuthGuard(http.HandlerFunc(handlers.Logout)))
	mux.Handle("/api/auth/me",           middleware.AuthGuard(http.HandlerFunc(handlers.Me)))

	// Plants (public)
	mux.HandleFunc("/api/plants",        handlers.Plants)
	mux.HandleFunc("/api/plants/",       handlers.Plants)

	// Admin Plants CRUD
	adminPlants := middleware.AdminGuard(middleware.AuthGuard(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case http.MethodGet:
			handlers.AdminListPlants(w, r)
		case http.MethodPost:
			handlers.AdminCreatePlant(w, r)
		case http.MethodPatch:
			handlers.AdminUpdatePlant(w, r)
		case http.MethodDelete:
			handlers.AdminDeletePlant(w, r)
		default:
			http.Error(w, "méthode non autorisée", http.StatusMethodNotAllowed)
		}
	})))
	mux.Handle("/api/admin/plants",  adminPlants)
	mux.Handle("/api/admin/plants/", adminPlants)

	// User routes (owner or admin)
	userRoute := middleware.OwnerGuard(middleware.AuthGuard(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case http.MethodGet:
			handlers.GetUser(w, r)
		case http.MethodPatch:
			handlers.UpdateUser(w, r)
		default:
			http.Error(w, "méthode non autorisée", http.StatusMethodNotAllowed)
		}
	})))
	mux.Handle("/api/users/", userRoute)

	// Admin Users CRUD
	adminUsers := middleware.AdminGuard(middleware.AuthGuard(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case http.MethodGet:
			handlers.AdminListUsers(w, r)
		case http.MethodPatch:
			handlers.AdminUpdateUser(w, r)
		case http.MethodDelete:
			handlers.AdminDeleteUser(w, r)
		default:
			http.Error(w, "méthode non autorisée", http.StatusMethodNotAllowed)
		}
	})))
	mux.Handle("/api/admin/users",  adminUsers)
	mux.Handle("/api/admin/users/", adminUsers)

	// Orders (user)
	userOrders := middleware.AuthGuard(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch {
		case r.URL.Path == "/api/orders" && r.Method == http.MethodGet:
			handlers.ListOrders(w, r)
		case r.URL.Path == "/api/orders" && r.Method == http.MethodPost:
			handlers.CreateOrder(w, r)
		case strings.HasPrefix(r.URL.Path, "/api/orders/") && r.Method == http.MethodGet:
			handlers.GetOrder(w, r)
		default:
			http.Error(w, "méthode non autorisée", http.StatusMethodNotAllowed)
		}
	}))
	mux.Handle("/api/orders",  userOrders)
	mux.Handle("/api/orders/", userOrders)

	// Admin Orders (PATCH & DELETE)
	adminOrders := middleware.AdminGuard(middleware.AuthGuard(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case http.MethodPatch:
			handlers.UpdateOrder(w, r)
		case http.MethodDelete:
			handlers.DeleteOrder(w, r)
		default:
			http.Error(w, "méthode non autorisée", http.StatusMethodNotAllowed)
		}
	})))
	mux.Handle("/api/orders/", adminOrders)

	return mux
}
