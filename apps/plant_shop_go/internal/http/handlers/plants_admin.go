package handlers

// ==============================================================================
// Importations
// ==============================================================================

import (
	"encoding/json"
	"net/http"

	"gorm.io/gorm"

	"plant_shop_go/internal/models"
)

// ==============================================================================
// Types
// ==============================================================================

// plantInput represente les donnees de creation de plante.
type plantInput struct {
	Name        string  `json:"name"`
	Price       float64 `json:"price"`
	Stock       int     `json:"stock"`
	Description string  `json:"description,omitempty"`
}

// plantUpdateInput represente les donnees de mise a jour de plante.
type plantUpdateInput struct {
	Name        *string  `json:"name,omitempty"`
	Price       *float64 `json:"price,omitempty"`
	Stock       *int     `json:"stock,omitempty"`
	Description *string  `json:"description,omitempty"`
}

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

// applyPlantUpdates applique les mises a jour sur une plante.
//
// @param targetPlant *models.Plant Plante a mettre a jour
// @param updateInput plantUpdateInput Donnees de mise a jour
func applyPlantUpdates(targetPlant *models.Plant, updateInput plantUpdateInput) {
	if updateInput.Name != nil {
		targetPlant.Name = *updateInput.Name
	}
	if updateInput.Price != nil {
		targetPlant.Price = roundPrice(*updateInput.Price)
	}
	if updateInput.Stock != nil {
		targetPlant.Stock = *updateInput.Stock
	}
	if updateInput.Description != nil {
		targetPlant.Description = *updateInput.Description
	}
}

// normalizePlantPrices arrondit les prix des plantes.
//
// @param plantList []models.Plant Liste des plantes a normaliser
func normalizePlantPrices(plantList []models.Plant) {
	for plantIndex := range plantList {
		plantList[plantIndex].Price = roundPrice(plantList[plantIndex].Price)
	}
}

// ==============================================================================
// Handlers
// ==============================================================================

// AdminListPlants liste toutes les plantes (route admin).
//
// @param gormDB *gorm.DB Client GORM
// @return http.HandlerFunc Handler HTTP
func AdminListPlants(gormDB *gorm.DB) http.HandlerFunc {
	return func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		var allPlants []models.Plant
		if queryError := gormDB.Order("name ASC").Find(&allPlants).Error; queryError != nil {
			http.Error(responseWriter, "db error", http.StatusInternalServerError)
			return
		}
		normalizePlantPrices(allPlants)
		json.NewEncoder(responseWriter).Encode(allPlants)
	}
}

// AdminCreatePlant cree une plante (route admin).
//
// @param gormDB *gorm.DB Client GORM
// @return http.HandlerFunc Handler HTTP
func AdminCreatePlant(gormDB *gorm.DB) http.HandlerFunc {
	return func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		var plantData plantInput
		if decodeError := json.NewDecoder(httpRequest.Body).Decode(&plantData); decodeError != nil {
			http.Error(responseWriter, "invalid json", http.StatusBadRequest)
			return
		}
		newPlant := models.Plant{
			Name:        plantData.Name,
			Price:       roundPrice(plantData.Price),
			Stock:       plantData.Stock,
			Description: plantData.Description,
		}
		if createError := gormDB.Create(&newPlant).Error; createError != nil {
			http.Error(responseWriter, "db error", http.StatusInternalServerError)
			return
		}
		responseWriter.WriteHeader(http.StatusCreated)
		json.NewEncoder(responseWriter).Encode(newPlant)
	}
}

// AdminUpdatePlant met a jour une plante par ID depuis le chemin.
//
// @param gormDB *gorm.DB Client GORM
// @return http.HandlerFunc Handler HTTP
func AdminUpdatePlant(gormDB *gorm.DB) http.HandlerFunc {
	return func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		plantID, parseError := parsePathID(httpRequest)
		if parseError != nil {
			http.Error(responseWriter, "invalid id", http.StatusBadRequest)
			return
		}
		var updateInput plantUpdateInput
		if decodeError := json.NewDecoder(httpRequest.Body).Decode(&updateInput); decodeError != nil {
			http.Error(responseWriter, "invalid json", http.StatusBadRequest)
			return
		}
		var targetPlant models.Plant
		if findError := gormDB.First(&targetPlant, plantID).Error; findError != nil {
			http.Error(responseWriter, "not found", http.StatusNotFound)
			return
		}
		applyPlantUpdates(&targetPlant, updateInput)
		gormDB.Save(&targetPlant)
		json.NewEncoder(responseWriter).Encode(targetPlant)
	}
}

// AdminDeletePlant supprime une plante par ID depuis le chemin.
//
// @param gormDB *gorm.DB Client GORM
// @return http.HandlerFunc Handler HTTP
func AdminDeletePlant(gormDB *gorm.DB) http.HandlerFunc {
	return func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		plantID, parseError := parsePathID(httpRequest)
		if parseError != nil {
			http.Error(responseWriter, "invalid id", http.StatusBadRequest)
			return
		}
		gormDB.Delete(&models.Plant{}, plantID)
		responseWriter.WriteHeader(http.StatusOK)
	}
}
