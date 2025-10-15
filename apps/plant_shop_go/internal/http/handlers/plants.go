package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"
	"strings"

	"plant_shop_go/internal/models"
	"gorm.io/gorm"
)

// GET /api/plants
func PublicListPlants(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var plants []models.Plant
		// Tri par nom, par ordre alphabétique.
		if err := db.Order("name ASC").Find(&plants).Error; err != nil { http.Error(w,"db error",500); return }
		for i := range plants {
			plants[i].Price = float64(int(plants[i].Price*100)) / 100.0
		}
		_ = json.NewEncoder(w).Encode(plants)
	}
}

// GET /api/plants/{id}
func PublicGetPlant(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		parts := strings.Split(r.URL.Path, "/")
		idStr := parts[len(parts)-1]
		id, err := strconv.Atoi(idStr); if err != nil { http.Error(w,"invalid id",400); return }
		var p models.Plant
		if err := db.First(&p, id).Error; err != nil { http.Error(w,"not found",404); return }
		p.Price = float64(int(p.Price*100)) / 100.0
		_ = json.NewEncoder(w).Encode(p)
	}
}
