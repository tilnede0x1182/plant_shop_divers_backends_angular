package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"
	"gorm.io/gorm"

	"plant_shop_go/internal/models"
	"plant_shop_go/internal/security"
)

// firstNonNil est un utilitaire pour gérer les deux formats de plantId.
func firstNonNil(a, b any) any {
	if a != nil {
		return a
	}
	return b
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

// ListUserOrders liste les commandes de l'utilisateur authentifié.
func ListUserOrders(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		raw := r.Context().Value("claims")
		if raw == nil {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
		claims := raw.(*security.Claims)
		var orders []models.Order
		if err := db.Where("user_id = ?", parseUintOrZero(claims.UserID)).Find(&orders).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}

		// *** LA CORRECTION EST ICI ***
		// Si aucune commande n'est trouvée, `orders` sera `nil`.
		// On s'assure de renvoyer un tableau JSON vide `[]` au lieu de `null`.
		if orders == nil {
			orders = make([]models.Order, 0)
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(orders)
	}
}

// CreateOrder crée une commande pour l'utilisateur authentifié.
func CreateOrder(db *gorm.DB) http.HandlerFunc {
	type itemReq struct {
		PlantIDAny any `json:"plantId"`
		PlantIDAlt any `json:"plant_id"`
		Quantity   int `json:"quantity"`
	}
	type reqBody struct {
		Items []itemReq `json:"items"`
	}

	return func(w http.ResponseWriter, r *http.Request) {
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

		order := models.Order{
			UserID:     parseUintOrZero(claims.UserID),
			TotalPrice: 0.0,
			Status:     "pending",
		}
		if err := db.Create(&order).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}

		var total float64 = 0.0
		for _, it := range in.Items {
			var plantID uint
			switch v := firstNonNil(it.PlantIDAny, it.PlantIDAlt).(type) {
			case string:
				if n, err := strconv.Atoi(v); err == nil {
					plantID = uint(n)
				}
			case float64:
				plantID = uint(v)
			case int:
				plantID = uint(v)
			}

			var p models.Plant
			if err := db.First(&p, plantID).Error; err != nil {
				db.Delete(&order)
				http.Error(w, "plant not found", http.StatusBadRequest)
				return
			}
			if p.Stock < it.Quantity {
				db.Delete(&order)
				http.Error(w, "out of stock", http.StatusBadRequest)
				return
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
			if err := db.Model(&p).Update("stock", p.Stock-it.Quantity).Error; err != nil {
				db.Delete(&order)
				http.Error(w, "db error", http.StatusInternalServerError)
				return
			}
			total += float64(it.Quantity) * p.Price
		}

		if err := db.Model(&order).Update("total_price", total).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}

		if err := db.Preload("Items").First(&order, order.ID).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		w.WriteHeader(http.StatusCreated)
		_ = json.NewEncoder(w).Encode(order)
	}
}

// UpdateOrder met à jour une commande (status).
func UpdateOrder(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		vars := mux.Vars(r)
		idStr, ok := vars["id"]
		if !ok {
			http.Error(w, "missing id", http.StatusBadRequest)
			return
		}
		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			http.Error(w, "invalid id", http.StatusBadRequest)
			return
		}
		var in struct {
			Status *string `json:"status,omitempty"`
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
		if err := db.Save(&order).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		_ = json.NewEncoder(w).Encode(order)
	}
}

// DeleteOrder supprime une commande par ID depuis le chemin.
func DeleteOrder(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		vars := mux.Vars(r)
		idStr, ok := vars["id"]
		if !ok {
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
		w.WriteHeader(http.StatusOK)
	}
}
