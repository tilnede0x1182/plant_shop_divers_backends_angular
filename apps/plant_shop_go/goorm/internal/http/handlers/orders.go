package handlers

import (
	"encoding/json"
	"net/http"
)

/*
POST /orders
Crée une commande (stub), nécessite auth.
*/
func CreateOrder(response http.ResponseWriter, request *http.Request) {
	type input struct {
		Items []struct {
			PlantID string `json:"plantId"`
			Qty     int    `json:"qty"`
		} `json:"items"`
	}
	var payload input
	json.NewDecoder(request.Body).Decode(&payload)
	json.NewEncoder(response).Encode(map[string]any{
		"status": "ok",
		"count":  len(payload.Items),
	})
}
