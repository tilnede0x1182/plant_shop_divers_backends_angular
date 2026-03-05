package handlers

// ==============================================================================
// Importations
// ==============================================================================

import (
	"encoding/json"
	"net/http"
	"strconv"
	"strings"

	"plant_shop_go/internal/models"
	"gorm.io/gorm"
)

// ==============================================================================
// Handlers publics
// ==============================================================================

// PublicListPlants retourne la liste de toutes les plantes.
//
// @param gormDB *gorm.DB Client GORM de base de donnees
// @return http.HandlerFunc Handler HTTP
func PublicListPlants(gormDB *gorm.DB) http.HandlerFunc {
	return func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		var allPlants []models.Plant
		if queryError := gormDB.Order("name ASC").Find(&allPlants).Error; queryError != nil {
			http.Error(responseWriter, "db error", 500)
			return
		}
		for plantIndex := range allPlants {
			allPlants[plantIndex].Price = float64(int(allPlants[plantIndex].Price*100)) / 100.0
		}
		_ = json.NewEncoder(responseWriter).Encode(allPlants)
	}
}

// PublicGetPlant retourne une plante par son ID.
//
// @param gormDB *gorm.DB Client GORM de base de donnees
// @return http.HandlerFunc Handler HTTP
func PublicGetPlant(gormDB *gorm.DB) http.HandlerFunc {
	return func(responseWriter http.ResponseWriter, httpRequest *http.Request) {
		pathParts := strings.Split(httpRequest.URL.Path, "/")
		plantIDString := pathParts[len(pathParts)-1]
		plantID, parseError := strconv.Atoi(plantIDString)
		if parseError != nil {
			http.Error(responseWriter, "invalid id", 400)
			return
		}
		var foundPlant models.Plant
		if findError := gormDB.First(&foundPlant, plantID).Error; findError != nil {
			http.Error(responseWriter, "not found", 404)
			return
		}
		foundPlant.Price = float64(int(foundPlant.Price*100)) / 100.0
		_ = json.NewEncoder(responseWriter).Encode(foundPlant)
	}
}
