package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"

	"plant_shop_go/internal/models"

	"gorm.io/gorm"
)

// AdminListPlants liste toutes les plantes (route admin).
func AdminListPlants(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var plants []models.Plant
		if err := db.Find(&plants).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		_ = json.NewEncoder(w).Encode(plants)
	}
}

// AdminCreatePlant crée une plante (route admin).
func AdminCreatePlant(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var in struct {
			Name        string  `json:"name"`
			Price       float64 `json:"price"`
			Stock       int     `json:"stock"`
			Description string  `json:"description,omitempty"`
		}
		if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
			http.Error(w, "invalid json", http.StatusBadRequest)
			return
		}
		p := models.Plant{
			Name:        in.Name,
			Price:       in.Price,
			Stock:       in.Stock,
			Description: in.Description,
		}
		if err := db.Create(&p).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		w.WriteHeader(http.StatusOK)
		_ = json.NewEncoder(w).Encode(p)
	}
}

// AdminUpdatePlant met à jour une plante par query param id.
func AdminUpdatePlant(db *gorm.DB) http.HandlerFunc {
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
			Name        *string  `json:"name,omitempty"`
			Price       *float64 `json:"price,omitempty"`
			Stock       *int     `json:"stock,omitempty"`
			Description *string  `json:"description,omitempty"`
		}
		if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
			http.Error(w, "invalid json", http.StatusBadRequest)
			return
		}
		var p models.Plant
		if err := db.First(&p, id).Error; err != nil {
			http.Error(w, "not found", http.StatusNotFound)
			return
		}
		if in.Name != nil {
			p.Name = *in.Name
		}
		if in.Price != nil {
			p.Price = *in.Price
		}
		if in.Stock != nil {
			p.Stock = *in.Stock
		}
		if in.Description != nil {
			p.Description = *in.Description
		}
		if err := db.Save(&p).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		_ = json.NewEncoder(w).Encode(p)
	}
}

// AdminDeletePlant supprime une plante par query param id.
func AdminDeletePlant(db *gorm.DB) http.HandlerFunc {
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
		if err := db.Delete(&models.Plant{}, id).Error; err != nil {
			http.Error(w, "db error", http.StatusInternalServerError)
			return
		}
		w.WriteHeader(http.StatusNoContent)
	}
}
