package handlers

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
func applyPlantUpdates(plant *models.Plant, in plantUpdateInput) {
	if in.Name != nil {
		plant.Name = *in.Name
	}
	if in.Price != nil {
		plant.Price = roundPrice(*in.Price)
	}
	if in.Stock != nil {
		plant.Stock = *in.Stock
	}
	if in.Description != nil {
		plant.Description = *in.Description
	}
}

// normalizePlantPrices arrondit les prix des plantes.
func normalizePlantPrices(plants []models.Plant) {
	for idx := range plants {
		plants[idx].Price = roundPrice(plants[idx].Price)
	}
}

// ==============================================================================
// Handlers
// ==============================================================================

// AdminListPlants liste toutes les plantes (route admin).
func AdminListPlants(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var plants []models.Plant
		if err := db.Order("name ASC").Find(&plants).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		normalizePlantPrices(plants)
		json.NewEncoder(w).Encode(plants)
	}
}

// AdminCreatePlant cree une plante (route admin).
func AdminCreatePlant(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var in plantInput
		if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
			http.Error(w, "invalid json", http.StatusBadRequest)
			return
		}
		plant := models.Plant{
			Name:        in.Name,
			Price:       roundPrice(in.Price),
			Stock:       in.Stock,
			Description: in.Description,
		}
		if err := db.Create(&plant).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		w.WriteHeader(http.StatusCreated)
		json.NewEncoder(w).Encode(plant)
	}
}

// AdminUpdatePlant met a jour une plante par ID depuis le chemin.
func AdminUpdatePlant(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		id, err := parsePathID(r)
		if err != nil {
			http.Error(w, "invalid id", http.StatusBadRequest)
			return
		}
		var in plantUpdateInput
		if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
			http.Error(w, "invalid json", http.StatusBadRequest)
			return
		}
		var plant models.Plant
		if err := db.First(&plant, id).Error; err != nil {
			http.Error(w, "not found", http.StatusNotFound)
			return
		}
		applyPlantUpdates(&plant, in)
		db.Save(&plant)
		json.NewEncoder(w).Encode(plant)
	}
}

// AdminDeletePlant supprime une plante par ID depuis le chemin.
func AdminDeletePlant(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		id, err := parsePathID(r)
		if err != nil {
			http.Error(w, "invalid id", http.StatusBadRequest)
			return
		}
		db.Delete(&models.Plant{}, id)
		w.WriteHeader(http.StatusOK)
	}
}
