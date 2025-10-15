package handlers

import (
	"encoding/json"
	"net/http"
	"strings"
)

/*
GET /plants et GET /plants/{id}
Stubs en attendant la DB.
*/
func Plants(response http.ResponseWriter, request *http.Request) {
	if request.URL.Path == "/plants" {
		json.NewEncoder(response).Encode([]map[string]any{
			{"id": "p1", "name": "Monstera"},
			{"id": "p2", "name": "Ficus"},
		})
		return
	}
	parts := strings.Split(strings.TrimPrefix(request.URL.Path, "/plants/"), "/")
	if len(parts) >= 1 && parts[0] != "" {
		json.NewEncoder(response).Encode(map[string]any{"id": parts[0], "name": "Plant " + parts[0]})
		return
	}
	http.NotFound(response, request)
}
