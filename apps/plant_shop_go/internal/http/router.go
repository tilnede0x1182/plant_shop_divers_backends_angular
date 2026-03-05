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

// setupAuthRoutes configure les routes d authentification.
//
// @param api *mux.Router Subrouter API
func setupAuthRoutes(api *mux.Router) {
	api.HandleFunc("/auth/login", handlers.Login).Methods("POST")
	api.HandleFunc("/auth/register", handlers.Register).Methods("POST")
	api.Handle("/auth/me", middleware.AuthGuard(http.HandlerFunc(handlers.Me))).Methods("GET")
	api.HandleFunc("/auth/logout", handlers.Logout).Methods("POST")
}

// setupPublicPlantRoutes configure les routes publiques plantes.
//
// @param api *mux.Router Subrouter API
// @param db *gorm.DB Client GORM
func setupPublicPlantRoutes(api *mux.Router, db *gorm.DB) {
	api.Handle("/plants", handlers.PublicListPlants(db)).Methods("GET")
	api.Handle("/plants/{id:[0-9]+}", handlers.PublicGetPlant(db)).Methods("GET")
}

// setupAdminPlantRoutes configure les routes admin plantes.
//
// @param api *mux.Router Subrouter API
// @param db *gorm.DB Client GORM
func setupAdminPlantRoutes(api *mux.Router, db *gorm.DB) {
	adminPlants := api.PathPrefix("/admin/plants").Subrouter()
	adminPlants.Use(middleware.AuthGuard, middleware.AdminGuard)
	adminPlants.Handle("", handlers.AdminListPlants(db)).Methods("GET")
	adminPlants.Handle("", handlers.AdminCreatePlant(db)).Methods("POST")
	adminPlants.Handle("/{id:[0-9]+}", handlers.AdminUpdatePlant(db)).Methods("PATCH")
	adminPlants.Handle("/{id:[0-9]+}", handlers.AdminDeletePlant(db)).Methods("DELETE")
}

// setupUserRoutes configure les routes utilisateurs.
//
// @param api *mux.Router Subrouter API
func setupUserRoutes(api *mux.Router) {
	api.Handle("/users", middleware.AuthGuard(middleware.AdminGuard(http.HandlerFunc(handlers.AdminListUsers)))).Methods("GET")
	api.Handle("/users", middleware.AuthGuard(middleware.AdminGuard(http.HandlerFunc(handlers.AdminCreateUser)))).Methods("POST")
	api.Handle("/users/{id:[0-9]+}", middleware.AuthGuard(middleware.OwnerGuard(http.HandlerFunc(handlers.GetUser)))).Methods("GET")
	api.Handle("/users/{id:[0-9]+}", middleware.AuthGuard(middleware.OwnerGuard(http.HandlerFunc(handlers.UpdateUser)))).Methods("PATCH")
	api.Handle("/users/{id:[0-9]+}", middleware.AuthGuard(middleware.AdminGuard(http.HandlerFunc(handlers.AdminDeleteUser)))).Methods("DELETE")
}

// setupAdminUserRoutes configure les routes admin utilisateurs.
//
// @param api *mux.Router Subrouter API
func setupAdminUserRoutes(api *mux.Router) {
	adminUsers := api.PathPrefix("/admin/users").Subrouter()
	adminUsers.Use(middleware.AuthGuard, middleware.AdminGuard)
	adminUsers.Handle("", http.HandlerFunc(handlers.AdminListUsers)).Methods("GET")
	adminUsers.Handle("/{id:[0-9]+}", http.HandlerFunc(handlers.AdminUpdateUser)).Methods("PATCH")
	adminUsers.Handle("/{id:[0-9]+}", http.HandlerFunc(handlers.AdminDeleteUser)).Methods("DELETE")
}

// setupOrderRoutes configure les routes commandes.
//
// @param api *mux.Router Subrouter API
// @param db *gorm.DB Client GORM
func setupOrderRoutes(api *mux.Router, db *gorm.DB) {
	orders := api.PathPrefix("/orders").Subrouter()
	orders.Use(middleware.AuthGuard)
	orders.Handle("", handlers.CreateOrder(db)).Methods("POST")
	orders.Handle("", handlers.ListUserOrders(db)).Methods("GET")
	orders.Handle("/{id:[0-9]+}", middleware.AdminGuard(handlers.UpdateOrder(db))).Methods("PATCH")
	orders.Handle("/{id:[0-9]+}", middleware.AdminGuard(handlers.DeleteOrder(db))).Methods("DELETE")
}

// setupIndexRoute configure la route index.
//
// @param r *mux.Router Routeur principal
func setupIndexRoute(r *mux.Router) {
	r.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("plant_shop_go OK"))
	})
}

// NewRouter construit et retourne le routeur principal.
//
// @param db *gorm.DB Client GORM de base de donnees
// @return *mux.Router Routeur configure
func NewRouter(db *gorm.DB) *mux.Router {
	r := mux.NewRouter()
	ensureAdminSeed(db)
	api := r.PathPrefix("/api").Subrouter()
	setupAuthRoutes(api)
	setupPublicPlantRoutes(api, db)
	setupAdminPlantRoutes(api, db)
	setupUserRoutes(api)
	setupAdminUserRoutes(api)
	setupOrderRoutes(api, db)
	setupIndexRoute(r)
	return r
}

// ensureAdminSeed cree l admin par defaut si absent.
//
// @param db *gorm.DB Client GORM de base de donnees
func ensureAdminSeed(db *gorm.DB) {
	var n int64
	db.Model(&models.User{}).Where("email = ?", "admin1@planteshop.com").Count(&n)
	if n == 0 {
		hash, _ := bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
		db.Create(&models.User{Email: "admin1@planteshop.com", Password: string(hash), Admin: true, Name: "Admin Test"})
	}
}
