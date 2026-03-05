package handlers

// ==============================================================================
// Importations
// ==============================================================================

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"
	"plant_shop_go/internal/db"
	"plant_shop_go/internal/models"
	"plant_shop_go/internal/security"
)

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

// parseUserID extrait l ID utilisateur depuis le chemin.
//
// @param httpRequest *http.Request Requete HTTP
// @return int ID utilisateur extrait
func parseUserID(httpRequest *http.Request) int {
	pathVars := mux.Vars(httpRequest)
	userID, _ := strconv.Atoi(pathVars["id"])
	return userID
}

// sanitizeUserInput retire le champ admin si non autorise.
//
// @param updateData map[string]any Donnees de mise a jour
// @param isAdminUser bool True si utilisateur admin
func sanitizeUserInput(updateData map[string]any, isAdminUser bool) {
	if !isAdminUser {
		delete(updateData, "admin")
	}
}

// fetchAndClearPassword charge un user et masque le mot de passe.
//
// @param targetUserID int ID de l utilisateur
// @return models.User Utilisateur trouve
// @return error Erreur si non trouve
func fetchAndClearPassword(targetUserID int) (models.User, error) {
	var foundUser models.User
	queryError := db.Connect().First(&foundUser, targetUserID).Error
	foundUser.Password = ""
	return foundUser, queryError
}

// ==============================================================================
// Handlers
// ==============================================================================

// GetUser retourne un utilisateur par son ID.
//
// @param responseWriter http.ResponseWriter Writer HTTP
// @param httpRequest *http.Request Requete HTTP
func GetUser(responseWriter http.ResponseWriter, httpRequest *http.Request) {
	targetUserID := parseUserID(httpRequest)
	foundUser, queryError := fetchAndClearPassword(targetUserID)
	if queryError != nil {
		http.Error(responseWriter, "user not found", http.StatusNotFound)
		return
	}
	json.NewEncoder(responseWriter).Encode(foundUser)
}

// UpdateUser met a jour un utilisateur (proprietaire ou admin).
//
// @param responseWriter http.ResponseWriter Writer HTTP
// @param httpRequest *http.Request Requete HTTP
func UpdateUser(responseWriter http.ResponseWriter, httpRequest *http.Request) {
	userClaims, claimsFound := httpRequest.Context().Value("claims").(*security.Claims)
	if !claimsFound {
		http.Error(responseWriter, "claims not found", http.StatusInternalServerError)
		return
	}
	targetUserID := parseUserID(httpRequest)
	var updateData map[string]any
	if decodeError := json.NewDecoder(httpRequest.Body).Decode(&updateData); decodeError != nil {
		http.Error(responseWriter, "bad request", http.StatusBadRequest)
		return
	}
	sanitizeUserInput(updateData, userClaims.Admin)
	gormDB := db.Connect()
	if updateError := gormDB.Model(&models.User{}).Where("id = ?", targetUserID).Updates(updateData).Error; updateError != nil {
		http.Error(responseWriter, "update failed", http.StatusInternalServerError)
		return
	}
	updatedUser, _ := fetchAndClearPassword(targetUserID)
	json.NewEncoder(responseWriter).Encode(updatedUser)
}
