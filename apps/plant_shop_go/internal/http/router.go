package httpserver

// ==============================================================================
// Importations
// ==============================================================================

import (
	"net/http"

	"plant_shop_go/internal/http/handlers"
	"plant_shop_go/internal/http/middleware"
	"plant_shop_go/internal/models"

	"github.com/gorilla/mux"
	"golang.org/x/crypto/bcrypt"
	"gorm.io/gorm"
)

// ==============================================================================
// Configuration des routes
// ==============================================================================

// setupAuthRoutes configure les routes d authentification.
//
// @param apiRouter *mux.Router Subrouter API
func setupAuthRoutes(apiRouter *mux.Router) {
	apiRouter.HandleFunc("/auth/login", handlers.Login).Methods("POST")
	apiRouter.HandleFunc("/auth/register", handlers.Register).Methods("POST")
	apiRouter.Handle("/auth/me", middleware.AuthGuard(http.HandlerFunc(handlers.Me))).Methods("GET")
	apiRouter.HandleFunc("/auth/logout", handlers.Logout).Methods("POST")
}

// setupPublicPlantRoutes configure les routes publiques plantes.
//
// @param apiRouter *mux.Router Subrouter API
// @param gormDB *gorm.DB Client GORM
func setupPublicPlantRoutes(apiRouter *mux.Router, gormDB *gorm.DB) {
	apiRouter.Handle("/plants", handlers.PublicListPlants(gormDB)).Methods("GET")
	apiRouter.Handle("/plants/{id:[0-9]+}", handlers.PublicGetPlant(gormDB)).Methods("GET")
}

// setupAdminPlantRoutes configure les routes admin plantes.
//
// @param apiRouter *mux.Router Subrouter API
// @param gormDB *gorm.DB Client GORM
func setupAdminPlantRoutes(apiRouter *mux.Router, gormDB *gorm.DB) {
	adminPlantsRouter := apiRouter.PathPrefix("/admin/plants").Subrouter()
	adminPlantsRouter.Use(middleware.AuthGuard, middleware.AdminGuard)
	adminPlantsRouter.Handle("", handlers.AdminListPlants(gormDB)).Methods("GET")
	adminPlantsRouter.Handle("", handlers.AdminCreatePlant(gormDB)).Methods("POST")
	adminPlantsRouter.Handle("/{id:[0-9]+}", handlers.AdminUpdatePlant(gormDB)).Methods("PATCH")
	adminPlantsRouter.Handle("/{id:[0-9]+}", handlers.AdminDeletePlant(gormDB)).Methods("DELETE")
}

// setupUserRoutes configure les routes utilisateurs.
//
// @param apiRouter *mux.Router Subrouter API
func setupUserRoutes(apiRouter *mux.Router) {
	apiRouter.Handle("/users", middleware.AuthGuard(middleware.AdminGuard(http.HandlerFunc(handlers.AdminListUsers)))).Methods("GET")
	apiRouter.Handle("/users", middleware.AuthGuard(middleware.AdminGuard(http.HandlerFunc(handlers.AdminCreateUser)))).Methods("POST")
	apiRouter.Handle("/users/{id:[0-9]+}", middleware.AuthGuard(middleware.OwnerGuard(http.HandlerFunc(handlers.GetUser)))).Methods("GET")
	apiRouter.Handle("/users/{id:[0-9]+}", middleware.AuthGuard(middleware.OwnerGuard(http.HandlerFunc(handlers.UpdateUser)))).Methods("PATCH")
	apiRouter.Handle("/users/{id:[0-9]+}", middleware.AuthGuard(middleware.AdminGuard(http.HandlerFunc(handlers.AdminDeleteUser)))).Methods("DELETE")
}

// setupAdminUserRoutes configure les routes admin utilisateurs.
//
// @param apiRouter *mux.Router Subrouter API
func setupAdminUserRoutes(apiRouter *mux.Router) {
	adminUsersRouter := apiRouter.PathPrefix("/admin/users").Subrouter()
	adminUsersRouter.Use(middleware.AuthGuard, middleware.AdminGuard)
	adminUsersRouter.Handle("", http.HandlerFunc(handlers.AdminListUsers)).Methods("GET")
	adminUsersRouter.Handle("/{id:[0-9]+}", http.HandlerFunc(handlers.AdminUpdateUser)).Methods("PATCH")
	adminUsersRouter.Handle("/{id:[0-9]+}", http.HandlerFunc(handlers.AdminDeleteUser)).Methods("DELETE")
}

// setupOrderRoutes configure les routes commandes.
//
// @param apiRouter *mux.Router Subrouter API
// @param gormDB *gorm.DB Client GORM
func setupOrderRoutes(apiRouter *mux.Router, gormDB *gorm.DB) {
	ordersRouter := apiRouter.PathPrefix("/orders").Subrouter()
	ordersRouter.Use(middleware.AuthGuard)
	ordersRouter.Handle("", handlers.CreateOrder(gormDB)).Methods("POST")
	ordersRouter.Handle("", handlers.ListUserOrders(gormDB)).Methods("GET")
	ordersRouter.Handle("/{id:[0-9]+}", middleware.AdminGuard(handlers.UpdateOrder(gormDB))).Methods("PATCH")
	ordersRouter.Handle("/{id:[0-9]+}", middleware.AdminGuard(handlers.DeleteOrder(gormDB))).Methods("DELETE")
}

// setupIndexRoute configure la route index.
//
// @param mainRouter *mux.Router Routeur principal
func setupIndexRoute(mainRouter *mux.Router) {
	mainRouter.HandleFunc("/", func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		responseWriter.WriteHeader(http.StatusOK)
		responseWriter.Write([]byte("plant_shop_go OK"))
	})
}

// ==============================================================================
// Constructeur du routeur
// ==============================================================================

// NewRouter construit et retourne le routeur principal.
//
// @param gormDB *gorm.DB Client GORM de base de donnees
// @return *mux.Router Routeur configure
func NewRouter(gormDB *gorm.DB) *mux.Router {
	mainRouter := mux.NewRouter()
	ensureAdminSeed(gormDB)
	apiRouter := mainRouter.PathPrefix("/api").Subrouter()
	setupAuthRoutes(apiRouter)
	setupPublicPlantRoutes(apiRouter, gormDB)
	setupAdminPlantRoutes(apiRouter, gormDB)
	setupUserRoutes(apiRouter)
	setupAdminUserRoutes(apiRouter)
	setupOrderRoutes(apiRouter, gormDB)
	setupIndexRoute(mainRouter)
	return mainRouter
}

// ==============================================================================
// Seed admin
// ==============================================================================

// ensureAdminSeed cree l admin par defaut si absent.
//
// @param gormDB *gorm.DB Client GORM de base de donnees
func ensureAdminSeed(gormDB *gorm.DB) {
	var adminCount int64
	gormDB.Model(&models.User{}).Where("email = ?", "admin1@planteshop.com").Count(&adminCount)
	if adminCount == 0 {
		passwordHash, _ := bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
		gormDB.Create(&models.User{Email: "admin1@planteshop.com", Password: string(passwordHash), Admin: true, Name: "Admin Test"})
	}
}
