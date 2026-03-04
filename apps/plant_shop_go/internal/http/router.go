package httpserver

import (
	"net/http"

	"plant_shop_go/internal/http/handlers"
	"plant_shop_go/internal/http/middleware"
	"plant_shop_go/internal/models"

	"github.com/gorilla/mux"
	"golang.org/x/crypto/bcrypt"
	"gorm.io/gorm"
)

// NewRouter construit et retourne le routeur principal.
// @param db Client GORM de base de données
func NewRouter(db *gorm.DB) *mux.Router {
	r := mux.NewRouter()
	ensureAdminSeed(db) // S'assure que l'admin du test existe

	api := r.PathPrefix("/api").Subrouter()

	// --- Auth ---
	// POST /api/auth/login : authentifie (email,password) et renvoie un cookie.
	// POST /api/auth/register : inscrit un user et renvoie un cookie.
	api.HandleFunc("/auth/login", handlers.Login).Methods("POST")
	api.HandleFunc("/auth/register", handlers.Register).Methods("POST")
	api.Handle("/auth/me", middleware.AuthGuard(http.HandlerFunc(handlers.Me))).Methods("GET")
	api.HandleFunc("/auth/logout", handlers.Logout).Methods("POST")

	// --- Public plants ---
	api.Handle("/plants", handlers.PublicListPlants(db)).Methods("GET")
	api.Handle("/plants/{id:[0-9]+}", handlers.PublicGetPlant(db)).Methods("GET")

	// --- Admin plants ---
	adminPlants := api.PathPrefix("/admin/plants").Subrouter()
	adminPlants.Use(middleware.AuthGuard, middleware.AdminGuard)
	adminPlants.Handle("", handlers.AdminListPlants(db)).Methods("GET")
	adminPlants.Handle("", handlers.AdminCreatePlant(db)).Methods("POST")
	adminPlants.Handle("/{id:[0-9]+}", handlers.AdminUpdatePlant(db)).Methods("PATCH")
	adminPlants.Handle("/{id:[0-9]+}", handlers.AdminDeletePlant(db)).Methods("DELETE")

	// --- Users ---
	api.Handle("/users", middleware.AuthGuard(middleware.AdminGuard(http.HandlerFunc(handlers.AdminListUsers)))).Methods("GET")
	api.Handle("/users", middleware.AuthGuard(middleware.AdminGuard(http.HandlerFunc(handlers.AdminCreateUser)))).Methods("POST")
	api.Handle("/users/{id:[0-9]+}", middleware.AuthGuard(middleware.OwnerGuard(http.HandlerFunc(handlers.GetUser)))).Methods("GET")
	api.Handle("/users/{id:[0-9]+}", middleware.AuthGuard(middleware.OwnerGuard(http.HandlerFunc(handlers.UpdateUser)))).Methods("PATCH")
	api.Handle("/users/{id:[0-9]+}", middleware.AuthGuard(middleware.AdminGuard(http.HandlerFunc(handlers.AdminDeleteUser)))).Methods("DELETE")

	// --- Admin Users (alias pour compatibilité) ---
	adminUsers := api.PathPrefix("/admin/users").Subrouter()
	adminUsers.Use(middleware.AuthGuard, middleware.AdminGuard)
	adminUsers.Handle("", http.HandlerFunc(handlers.AdminListUsers)).Methods("GET")
	adminUsers.Handle("/{id:[0-9]+}", http.HandlerFunc(handlers.AdminUpdateUser)).Methods("PATCH")
	adminUsers.Handle("/{id:[0-9]+}", http.HandlerFunc(handlers.AdminDeleteUser)).Methods("DELETE")

	// --- Orders ---
	orders := api.PathPrefix("/orders").Subrouter()
	orders.Use(middleware.AuthGuard)
	orders.Handle("", handlers.CreateOrder(db)).Methods("POST")
	orders.Handle("", handlers.ListUserOrders(db)).Methods("GET")
	orders.Handle("/{id:[0-9]+}", middleware.AdminGuard(handlers.UpdateOrder(db))).Methods("PATCH")
	orders.Handle("/{id:[0-9]+}", middleware.AdminGuard(handlers.DeleteOrder(db))).Methods("DELETE")

	// --- Route Index ---
	r.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("plant_shop_go OK"))
	})

	return r
}

// ensureAdminSeed crée l'admin par défaut si absent
func ensureAdminSeed(db *gorm.DB) {
	var n int64
	db.Model(&models.User{}).Where("email = ?", "admin1@planteshop.com").Count(&n)
	if n == 0 {
		hash, _ := bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
		db.Create(&models.User{Email: "admin1@planteshop.com", Password: string(hash), Admin: true, Name: "Admin Test"})
	}
}
