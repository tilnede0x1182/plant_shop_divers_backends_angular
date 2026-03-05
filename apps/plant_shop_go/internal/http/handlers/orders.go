package handlers

// ==============================================================================
// Importations
// ==============================================================================

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

/// firstNonNil retourne la premiere valeur non nil.
//
// @param firstValue any Premiere valeur a tester
// @param secondValue any Seconde valeur fallback
// @return any Premiere non nil
func firstNonNil(firstValue, secondValue any) any {
	if firstValue != nil {
		return firstValue
	}
	return secondValue
}

// parseUintOrZero convertit une string en uint.
//
// @param stringValue string Chaine a convertir
// @return uint Valeur ou 0
func parseUintOrZero(stringValue string) uint {
	if stringValue == "" {
		return 0
	}
	parsedValue, parseError := strconv.ParseUint(stringValue, 10, 64)
	if parseError != nil {
		return 0
	}
	return uint(parsedValue)
}

// roundPrice arrondit un prix a 2 decimales.
func roundPrice(price float64) float64 {
	return math.Round(price*100) / 100.0
}

// extractClaims extrait les claims du contexte.
//
// @param httpRequest *http.Request Requete HTTP
// @return *security.Claims Claims extraits ou nil
// @return bool True si claims trouves
func extractClaims(httpRequest *http.Request) (*security.Claims, bool) {
	rawClaims := httpRequest.Context().Value("claims")
	if rawClaims == nil {
		return nil, false
	}
	userClaims, claimsFound := rawClaims.(*security.Claims)
	return userClaims, claimsFound
}

// parsePathID extrait l ID depuis le chemin URL.
//
// @param httpRequest *http.Request Requete HTTP
// @return uint64 ID extrait
// @return error Erreur si ID manquant
func parsePathID(httpRequest *http.Request) (uint64, error) {
	pathVars := mux.Vars(httpRequest)
	idString, idFound := pathVars["id"]
	if !idFound {
		return 0, errors.New("missing id")
	}
	return strconv.ParseUint(idString, 10, 64)
}

/// extractPlantID extrait l ID de plante depuis un itemReq.
//
// @param orderItemInput itemReq Item de commande
// @return uint ID de la plante
func extractPlantID(orderItemInput itemReq) uint {
	switch rawValue := firstNonNil(orderItemInput.PlantIDAny, orderItemInput.PlantIDAlt).(type) {
	case string:
		if parsedNum, parseError := strconv.ParseFloat(rawValue, 64); parseError == nil {
			return uint(parsedNum)
		}
	case float64:
		return uint(rawValue)
	case int:
		return uint(rawValue)
	}
	return 0
}

// validateOrderInput valide le body de creation de commande.
//
// @param httpRequest *http.Request Requete HTTP
// @return *reqBody Body decode ou nil
// @return error Erreur de validation
func validateOrderInput(httpRequest *http.Request) (*reqBody, error) {
	var orderBody reqBody
	if decodeError := json.NewDecoder(httpRequest.Body).Decode(&orderBody); decodeError != nil {
		return nil, errors.New("invalid json")
	}
	if len(orderBody.Items) == 0 {
		return nil, errors.New("no items")
	}
	return &orderBody, nil
}

// createEmptyOrder cree une commande vide pour un utilisateur.
//
// @param gormDB *gorm.DB Client GORM
// @param userID string ID utilisateur
// @return *models.Order Commande creee ou nil
// @return error Erreur de creation
func createEmptyOrder(gormDB *gorm.DB, userID string) (*models.Order, error) {
	newOrder := models.Order{UserID: parseUintOrZero(userID), TotalPrice: 0.0, Status: "pending"}
	if createError := gormDB.Create(&newOrder).Error; createError != nil {
		return nil, createError
	}
	return &newOrder, nil
}

// ==============================================================================
// Fonctions utilitaires principales
// ==============================================================================

// filterOrderItems filtre les items sans plante valide.
//
// @param orderList []models.Order Liste des commandes a filtrer
func filterOrderItems(orderList []models.Order) {
	for orderIndex := range orderList {
		filteredItems := orderList[orderIndex].Items[:0]
		for _, orderItem := range orderList[orderIndex].Items {
			if orderItem.Plant.ID == 0 {
				continue
			}
			orderItem.Plant.Price = roundPrice(orderItem.Plant.Price)
			filteredItems = append(filteredItems, orderItem)
		}
		orderList[orderIndex].Items = filteredItems
	}
}

// fetchUserOrders recupere les commandes d un utilisateur.
//
// @param gormDB *gorm.DB Client GORM
// @param userID string ID utilisateur
// @return []models.Order Liste des commandes
// @return error Erreur de requete
func fetchUserOrders(gormDB *gorm.DB, userID string) ([]models.Order, error) {
	var userOrders []models.Order
	queryError := gormDB.Preload("Items.Plant").
		Where("user_id = ?", parseUintOrZero(userID)).
		Order("created_at DESC").
		Find(&userOrders).Error
	if queryError != nil {
		return nil, queryError
	}
	if userOrders == nil {
		userOrders = make([]models.Order, 0)
	}
	return userOrders, nil
}

// createOrderItem cree un item et met a jour le stock.
//
// @param gormDB *gorm.DB Client GORM
// @param targetOrderID uint ID de la commande
// @param itemInput itemReq Item a creer
// @return float64 Prix total de l item
// @return error Erreur de creation
func createOrderItem(gormDB *gorm.DB, targetOrderID uint, itemInput itemReq) (float64, error) {
	targetPlantID := extractPlantID(itemInput)
	var targetPlant models.Plant
	if findError := gormDB.First(&targetPlant, targetPlantID).Error; findError != nil {
		return 0, errors.New("plant not found")
	}
	if targetPlant.Stock < itemInput.Quantity {
		return 0, errors.New("out of stock")
	}
	newOrderItem := models.OrderItem{OrderID: targetOrderID, PlantID: targetPlant.ID, Quantity: itemInput.Quantity}
	if createError := gormDB.Create(&newOrderItem).Error; createError != nil {
		return 0, createError
	}
	gormDB.Model(&targetPlant).Update("stock", targetPlant.Stock-itemInput.Quantity)
	return float64(itemInput.Quantity) * targetPlant.Price, nil
}

// processOrderItems traite tous les items d une commande.
//
// @param gormDB *gorm.DB Client GORM
// @param targetOrderID uint ID de la commande
// @param itemsList []itemReq Liste des items
// @return float64 Total de la commande
// @return error Erreur de traitement
func processOrderItems(gormDB *gorm.DB, targetOrderID uint, itemsList []itemReq) (float64, error) {
	var orderTotal float64
	for _, itemInput := range itemsList {
		itemPrice, itemError := createOrderItem(gormDB, targetOrderID, itemInput)
		if itemError != nil {
			return 0, itemError
		}
		orderTotal += itemPrice
	}
	return orderTotal, nil
}

// finalizeOrder met a jour le total et charge les items.
//
// @param gormDB *gorm.DB Client GORM
// @param targetOrder *models.Order Commande a finaliser
// @param orderTotal float64 Total calcule
// @return error Toujours nil
func finalizeOrder(gormDB *gorm.DB, targetOrder *models.Order, orderTotal float64) error {
	roundedTotal := roundPrice(orderTotal)
	gormDB.Model(targetOrder).Update("total_price", roundedTotal)
	gormDB.Preload("Items").First(targetOrder, targetOrder.ID)
	targetOrder.TotalPrice = roundPrice(targetOrder.TotalPrice)
	return nil
}

// ==============================================================================
// Handlers principaux
// ==============================================================================

// ListUserOrders liste les commandes de l utilisateur authentifie.
//
// @param gormDB *gorm.DB Client GORM
// @return http.HandlerFunc Handler HTTP
func ListUserOrders(gormDB *gorm.DB) http.HandlerFunc {
	return func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		userClaims, claimsFound := extractClaims(httpRequest)
		if !claimsFound {
			http.Error(responseWriter, "unauthorized", http.StatusUnauthorized)
			return
		}
		userOrders, queryError := fetchUserOrders(gormDB, userClaims.UserID)
		if queryError != nil {
			http.Error(responseWriter, "db error", http.StatusInternalServerError)
			return
		}
		filterOrderItems(userOrders)
		responseWriter.Header().Set("Content-Type", "application/json")
		json.NewEncoder(responseWriter).Encode(userOrders)
	}
}

// CreateOrder cree une commande pour l utilisateur authentifie.
//
// @param gormDB *gorm.DB Client GORM
// @return http.HandlerFunc Handler HTTP
func CreateOrder(gormDB *gorm.DB) http.HandlerFunc {
	return func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		userClaims, claimsFound := extractClaims(httpRequest)
		if !claimsFound {
			http.Error(responseWriter, "unauthorized", http.StatusUnauthorized)
			return
		}
		orderInput, validationError := validateOrderInput(httpRequest)
		if validationError != nil {
			http.Error(responseWriter, validationError.Error(), http.StatusBadRequest)
			return
		}
		newOrder, createError := createEmptyOrder(gormDB, userClaims.UserID)
		if createError != nil {
			http.Error(responseWriter, "db error", http.StatusInternalServerError)
			return
		}
		orderTotal, processError := processOrderItems(gormDB, newOrder.ID, orderInput.Items)
		if processError != nil {
			gormDB.Delete(newOrder)
			http.Error(responseWriter, processError.Error(), http.StatusBadRequest)
			return
		}
		finalizeOrder(gormDB, newOrder, orderTotal)
		responseWriter.WriteHeader(http.StatusCreated)
		json.NewEncoder(responseWriter).Encode(newOrder)
	}
}

// orderStatusInput structure pour mise a jour status.
type orderStatusInput struct {
	Status *string `json:"status,omitempty"`
}

// updateOrderStatus applique le nouveau status si present.
//
// @param targetOrder *models.Order Commande a mettre a jour
// @param statusInput *orderStatusInput Input de mise a jour
func updateOrderStatus(targetOrder *models.Order, statusInput *orderStatusInput) {
	if statusInput.Status != nil {
		targetOrder.Status = *statusInput.Status
	}
}

// UpdateOrder met a jour une commande (status).
//
// @param gormDB *gorm.DB Client GORM
// @return http.HandlerFunc Handler HTTP
func UpdateOrder(gormDB *gorm.DB) http.HandlerFunc {
	return func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		orderID, parseError := parsePathID(httpRequest)
		if parseError != nil {
			http.Error(responseWriter, "invalid id", http.StatusBadRequest)
			return
		}
		var statusInput orderStatusInput
		if decodeError := json.NewDecoder(httpRequest.Body).Decode(&statusInput); decodeError != nil {
			http.Error(responseWriter, "invalid json", http.StatusBadRequest)
			return
		}
		var targetOrder models.Order
		if findError := gormDB.First(&targetOrder, orderID).Error; findError != nil {
			http.Error(responseWriter, "not found", http.StatusNotFound)
			return
		}
		updateOrderStatus(&targetOrder, &statusInput)
		gormDB.Save(&targetOrder)
		json.NewEncoder(responseWriter).Encode(targetOrder)
	}
}

// DeleteOrder supprime une commande par ID depuis le chemin.
//
// @param gormDB *gorm.DB Client GORM
// @return http.HandlerFunc Handler HTTP
func DeleteOrder(gormDB *gorm.DB) http.HandlerFunc {
	return func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		orderID, parseError := parsePathID(httpRequest)
		if parseError != nil {
			http.Error(responseWriter, "invalid id", http.StatusBadRequest)
			return
		}
		gormDB.Delete(&models.Order{}, orderID)
		responseWriter.WriteHeader(http.StatusOK)
	}
}
