package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"

	"goorm/internal/db"
	"goorm/internal/models"
)

/*
GET /api/admin/plants
*/
func AdminListPlants(w http.ResponseWriter, r *http.Request) {
	d := db.Connect()
	var list []models.Plant
	d.Find(&list)
	json.NewEncoder(w).Encode(list)
}

/*
POST /api/admin/plants
*/
func AdminCreatePlant(w http.ResponseWriter, r *http.Request) {
	var in struct {
		Name  string `json:"name"`
		Price int    `json:"price"`
		Stock int    `json:"stock"`
	}
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		http.Error(w, "bad request", http.StatusBadRequest)
		return
	}
	p := models.Plant{Name: in.Name, Price: in.Price, Stock: in.Stock}
	db.Connect().Create(&p)
	json.NewEncoder(w).Encode(p)
}

/*
PATCH /api/admin/plants/{id}
*/
func AdminUpdatePlant(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(r.URL.Path, "/")
	id, _ := strconv.Atoi(parts[4])
	var in map[string]any
	json.NewDecoder(r.Body).Decode(&in)
	d := db.Connect()
	d.Model(&models.Plant{}).Where("id = ?", id).Updates(in)
	var p models.Plant
	d.First(&p, id)
	json.NewEncoder(w).Encode(p)
}

/*
DELETE /api/admin/plants/{id}
*/
func AdminDeletePlant(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(r.URL.Path, "/")
	id, _ := strconv.Atoi(parts[4])
	db.Connect().Delete(&models.Plant{}, id)
	w.WriteHeader(http.StatusNoContent)
}
