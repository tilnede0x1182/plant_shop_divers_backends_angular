package handlers

import (
	"encoding/json"
	"errors"
	"math"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"
	"gorm.io/gorm"

	"plant_shop_go/internal/models"
	"plant_shop_go/internal/security"
)

// ==============================================================================
// Types
// ==============================================================================

// itemReq represente un item de commande en entree.
type itemReq struct {
	PlantIDAny any `json:"plantId"`
	PlantIDAlt any `json:"plant_id"`
	Quantity   int `json:"quantity"`
}

// reqBody represente le corps de requete de creation de commande.
type reqBody struct {
	Items []itemReq `json:"items"`
}

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

// firstNonNil retourne la premiere valeur non nil.
func firstNonNil(a, b any) any {
	if a != nil {
		return a
	}
	return b
}

// parseUintOrZero convertit une string en uint.
func parseUintOrZero(s string) uint {
	if s == "" {
		return 0
	}
	val, err := strconv.ParseUint(s, 10, 64)
	if err != nil {
		return 0
	}
	return uint(val)
}

// roundPrice arrondit un prix a 2 decimales.
func roundPrice(price float64) float64 {
	return math.Round(price*100) / 100.0
}

// extractClaims extrait les claims du contexte.
func extractClaims(r *http.Request) (*security.Claims, bool) {
	raw := r.Context().Value("claims")
	if raw == nil {
		return nil, false
	}
	claims, ok := raw.(*security.Claims)
	return claims, ok
}

// parsePathID extrait l ID depuis le chemin URL.
func parsePathID(r *http.Request) (uint64, error) {
	vars := mux.Vars(r)
	idStr, ok := vars["id"]
	if !ok {
		return 0, errors.New("missing id")
	}
	return strconv.ParseUint(idStr, 10, 64)
}

// extractPlantID extrait l ID de plante depuis un itemReq.
func extractPlantID(it itemReq) uint {
	switch val := firstNonNil(it.PlantIDAny, it.PlantIDAlt).(type) {
	case string:
		if num, err := strconv.ParseFloat(val, 64); err == nil {
			return uint(num)
		}
	case float64:
		return uint(val)
	case int:
		return uint(val)
	}
	return 0
}

// validateOrderInput valide le body de creation de commande.
//
// @param r *http.Request Requete HTTP
// @return *reqBody Body decode ou nil
// @return error Erreur de validation
func validateOrderInput(r *http.Request) (*reqBody, error) {
	var in reqBody
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		return nil, errors.New("invalid json")
	}
	if len(in.Items) == 0 {
		return nil, errors.New("no items")
	}
	return &in, nil
}

// createEmptyOrder cree une commande vide pour un utilisateur.
//
// @param db *gorm.DB Client GORM
// @param userID string ID utilisateur
// @return *models.Order Commande creee ou nil
// @return error Erreur de creation
func createEmptyOrder(db *gorm.DB, userID string) (*models.Order, error) {
	order := models.Order{UserID: parseUintOrZero(userID), TotalPrice: 0.0, Status: "pending"}
	if err := db.Create(&order).Error; err != nil {
		return nil, err
	}
	return &order, nil
}

// ==============================================================================
// Fonctions utilitaires principales
// ==============================================================================

// filterOrderItems filtre les items sans plante valide.
func filterOrderItems(orders []models.Order) {
	for idx := range orders {
		filtered := orders[idx].Items[:0]
		for _, item := range orders[idx].Items {
			if item.Plant.ID == 0 {
				continue
			}
			item.Plant.Price = roundPrice(item.Plant.Price)
			filtered = append(filtered, item)
		}
		orders[idx].Items = filtered
	}
}

// fetchUserOrders recupere les commandes d un utilisateur.
func fetchUserOrders(db *gorm.DB, userID string) ([]models.Order, error) {
	var orders []models.Order
	err := db.Preload("Items.Plant").
		Where("user_id = ?", parseUintOrZero(userID)).
		Order("created_at DESC").
		Find(&orders).Error
	if err != nil {
		return nil, err
	}
	if orders == nil {
		orders = make([]models.Order, 0)
	}
	return orders, nil
}

// createOrderItem cree un item et met a jour le stock.
func createOrderItem(db *gorm.DB, orderID uint, it itemReq) (float64, error) {
	plantID := extractPlantID(it)
	var plant models.Plant
	if err := db.First(&plant, plantID).Error; err != nil {
		return 0, errors.New("plant not found")
	}
	if plant.Stock < it.Quantity {
		return 0, errors.New("out of stock")
	}
	orderItem := models.OrderItem{OrderID: orderID, PlantID: plant.ID, Quantity: it.Quantity}
	if err := db.Create(&orderItem).Error; err != nil {
		return 0, err
	}
	db.Model(&plant).Update("stock", plant.Stock-it.Quantity)
	return float64(it.Quantity) * plant.Price, nil
}

// processOrderItems traite tous les items d une commande.
func processOrderItems(db *gorm.DB, orderID uint, items []itemReq) (float64, error) {
	var total float64
	for _, it := range items {
		itemTotal, err := createOrderItem(db, orderID, it)
		if err != nil {
			return 0, err
		}
		total += itemTotal
	}
	return total, nil
}

// finalizeOrder met a jour le total et charge les items.
func finalizeOrder(db *gorm.DB, order *models.Order, total float64) error {
	total = roundPrice(total)
	db.Model(order).Update("total_price", total)
	db.Preload("Items").First(order, order.ID)
	order.TotalPrice = roundPrice(order.TotalPrice)
	return nil
}

// ==============================================================================
// Handlers principaux
// ==============================================================================

// ListUserOrders liste les commandes de l utilisateur authentifie.
func ListUserOrders(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		claims, ok := extractClaims(r)
		if !ok {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
		orders, err := fetchUserOrders(db, claims.UserID)
		if err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		filterOrderItems(orders)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(orders)
	}
}

// CreateOrder cree une commande pour l utilisateur authentifie.
func CreateOrder(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		claims, ok := extractClaims(r)
		if !ok {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
		in, err := validateOrderInput(r)
		if err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}
		order, err := createEmptyOrder(db, claims.UserID)
		if err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		total, err := processOrderItems(db, order.ID, in.Items)
		if err != nil {
			db.Delete(order)
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}
		finalizeOrder(db, order, total)
		w.WriteHeader(http.StatusCreated)
		json.NewEncoder(w).Encode(order)
	}
}

// orderStatusInput structure pour mise a jour status.
type orderStatusInput struct {
	Status *string `json:"status,omitempty"`
}

// updateOrderStatus applique le nouveau status si present.
//
// @param order *models.Order Commande a mettre a jour
// @param in *orderStatusInput Input de mise a jour
func updateOrderStatus(order *models.Order, in *orderStatusInput) {
	if in.Status != nil {
		order.Status = *in.Status
	}
}

// UpdateOrder met a jour une commande (status).
func UpdateOrder(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		id, err := parsePathID(r)
		if err != nil {
			http.Error(w, "invalid id", http.StatusBadRequest)
			return
		}
		var in orderStatusInput
		if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
			http.Error(w, "invalid json", http.StatusBadRequest)
			return
		}
		var order models.Order
		if err := db.First(&order, id).Error; err != nil {
			http.Error(w, "not found", http.StatusNotFound)
			return
		}
		updateOrderStatus(&order, &in)
		db.Save(&order)
		json.NewEncoder(w).Encode(order)
	}
}

// DeleteOrder supprime une commande par ID depuis le chemin.
func DeleteOrder(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		id, err := parsePathID(r)
		if err != nil {
			http.Error(w, "invalid id", http.StatusBadRequest)
			return
		}
		db.Delete(&models.Order{}, id)
		w.WriteHeader(http.StatusOK)
	}
}
