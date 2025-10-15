package httpserver

import (
	"plant_shop_go/internal/http/handlers"
	"plant_shop_go/internal/http/middleware"

	"github.com/gorilla/mux"
	"gorm.io/gorm"
	"net/http"
)

// NewRouter construit et retourne le routeur principal.
// Il attend que les handlers soient des constructeurs: func(*gorm.DB) http.HandlerFunc
func NewRouter(db *gorm.DB) *mux.Router {
	r := mux.NewRouter()

	// Admin plants
	r.Handle("/api/admin/plants",
		middleware.AdminGuard(middleware.AuthGuard(handlers.AdminListPlants(db))),
	).Methods("GET")
	r.Handle("/api/admin/plants",
		middleware.AdminGuard(middleware.AuthGuard(handlers.AdminCreatePlant(db))),
	).Methods("POST")
	r.Handle("/api/admin/plants",
		middleware.AdminGuard(middleware.AuthGuard(handlers.AdminUpdatePlant(db))),
	).Methods("PATCH")
	r.Handle("/api/admin/plants",
		middleware.AdminGuard(middleware.AuthGuard(handlers.AdminDeletePlant(db))),
	).Methods("DELETE")

	// Orders (admin and user)
	// Liste des commandes (admin)
	r.Handle("/api/admin/orders",
		middleware.AdminGuard(middleware.AuthGuard(handlers.ListOrders(db))),
	).Methods("GET")

	// Création d'une commande (user authentifié)
	r.Handle("/api/orders",
		middleware.AuthGuard(handlers.CreateOrder(db)),
	).Methods("POST")

	// Détails / modification / suppression d'une commande (admin)
	r.Handle("/api/admin/orders/get",
		middleware.AdminGuard(middleware.AuthGuard(handlers.GetOrder(db))),
	).Methods("GET")
	r.Handle("/api/admin/orders",
		middleware.AdminGuard(middleware.AuthGuard(handlers.UpdateOrder(db))),
	).Methods("PATCH")
	r.Handle("/api/admin/orders",
		middleware.AdminGuard(middleware.AuthGuard(handlers.DeleteOrder(db))),
	).Methods("DELETE")

	// Route POST /auth/login : authentifie un utilisateur avec email et mot de passe, renvoie un cookie de session (JWT).
	// Route POST /auth/register : inscrit un nouvel utilisateur, renvoie un cookie de session (JWT).
	r.HandleFunc("/auth/login", handlers.Login).Methods("POST")
	r.HandleFunc("/auth/register", handlers.Register).Methods("POST")

	// Route index pour test rapide
	r.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusCreated)
		w.Write([]byte("plant_shop_go OK"))
	})

	return r
}
