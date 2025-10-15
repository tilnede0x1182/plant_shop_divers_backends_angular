package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"

	"plant_shop_go/internal/models"
	"plant_shop_go/internal/security"

	"gorm.io/gorm"
)

// ListOrders liste toutes les commandes (admin).
func ListOrders(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var orders []models.Order
		if err := db.Find(&orders).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		_ = json.NewEncoder(w).Encode(orders)
	}
}

// CreateOrder crée une commande pour l'utilisateur authentifié.
// Corps attendu : { "items": [{"plant_id": 1, "quantity": 2}, ...] }
func CreateOrder(db *gorm.DB) http.HandlerFunc {
	type itemReq struct {
		PlantID  uint `json:"plant_id"`
		Quantity int  `json:"quantity"`
	}
	type reqBody struct {
		Items []itemReq `json:"items"`
	}

	return func(w http.ResponseWriter, r *http.Request) {
		// Récupérer claims injectés par AuthGuard
		raw := r.Context().Value("claims")
		if raw == nil {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
		claims, ok := raw.(*security.Claims)
		if !ok {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		var in reqBody
		if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
			http.Error(w, "invalid json", http.StatusBadRequest)
			return
		}
		if len(in.Items) == 0 {
			http.Error(w, "no items", http.StatusBadRequest)
			return
		}

		// Créer la commande
		order := models.Order{
			UserID:     parseUintOrZero(claims.UserID),
			TotalPrice: 0.0,
			Status:     "pending",
		}
		if err := db.Create(&order).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}

		// Pour chaque item, vérifier stock, créer OrderItem et ajuster stock et total
		var total float64 = 0.0
		for _, it := range in.Items {
			var p models.Plant
			if err := db.First(&p, it.PlantID).Error; err != nil {
				// rollback minimal : supprimer la commande créée et renvoyer erreur
				db.Delete(&order)
				http.Error(w, "plant not found", http.StatusBadRequest)
				return
			}
			if p.Stock <= 0 || it.Quantity <= 0 {
				db.Delete(&order)
				http.Error(w, "invalid quantity or out of stock", http.StatusBadRequest)
				return
			}
			if it.Quantity > p.Stock {
				it.Quantity = p.Stock
			}
			oi := models.OrderItem{
				OrderID:  order.ID,
				PlantID:  p.ID,
				Quantity: it.Quantity,
			}
			if err := db.Create(&oi).Error; err != nil {
				db.Delete(&order)
				http.Error(w, "db error", http.StatusInternalServerError)
				return
			}
			// Mettre à jour le stock
			if err := db.Model(&p).Update("stock", p.Stock-it.Quantity).Error; err != nil {
				db.Delete(&order)
				http.Error(w, "db error", http.StatusInternalServerError)
				return
			}
			total += float64(it.Quantity) * p.Price
		}

		// Mettre à jour le total de la commande
		if err := db.Model(&order).Update("total_price", total).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}

		// Recharger la commande pour renvoyer
		if err := db.First(&order, order.ID).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		w.WriteHeader(http.StatusOK)
		_ = json.NewEncoder(w).Encode(order)
	}
}

// GetOrder retourne une commande par query param id.
func GetOrder(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		idStr := r.URL.Query().Get("id")
		if idStr == "" {
			http.Error(w, "missing id", http.StatusBadRequest)
			return
		}
		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			http.Error(w, "invalid id", http.StatusBadRequest)
			return
		}
		var order models.Order
		if err := db.First(&order, id).Error; err != nil {
			http.Error(w, "not found", http.StatusNotFound)
			return
		}
		_ = json.NewEncoder(w).Encode(order)
	}
}

// UpdateOrder met à jour une commande (status et/ou total_price).
func UpdateOrder(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		idStr := r.URL.Query().Get("id")
		if idStr == "" {
			http.Error(w, "missing id", http.StatusBadRequest)
			return
		}
		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			http.Error(w, "invalid id", http.StatusBadRequest)
			return
		}
		var in struct {
			Status     *string  `json:"status,omitempty"`
			TotalPrice *float64 `json:"total_price,omitempty"`
		}
		if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
			http.Error(w, "invalid json", http.StatusBadRequest)
			return
		}
		var order models.Order
		if err := db.First(&order, id).Error; err != nil {
			http.Error(w, "not found", http.StatusNotFound)
			return
		}
		if in.Status != nil {
			order.Status = *in.Status
		}
		if in.TotalPrice != nil {
			order.TotalPrice = *in.TotalPrice
		}
		if err := db.Save(&order).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		_ = json.NewEncoder(w).Encode(order)
	}
}

// DeleteOrder supprime une commande par query param id.
func DeleteOrder(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		idStr := r.URL.Query().Get("id")
		if idStr == "" {
			http.Error(w, "missing id", http.StatusBadRequest)
			return
		}
		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			http.Error(w, "invalid id", http.StatusBadRequest)
			return
		}
		if err := db.Delete(&models.Order{}, id).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		w.WriteHeader(http.StatusNoContent)
	}
}

// parseUintOrZero tente de convertir une string en uint; retourne 0 si échec.
func parseUintOrZero(s string) uint {
	if s == "" {
		return 0
	}
	v, err := strconv.ParseUint(s, 10, 64)
	if err != nil {
		return 0
	}
	return uint(v)
}
