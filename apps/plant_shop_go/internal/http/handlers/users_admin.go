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

	"golang.org/x/crypto/bcrypt"
	"strings"
)

// ==============================================================================
// Types
// ==============================================================================

// adminUserInput structure pour creation/update user admin.
type adminUserInput struct {
	Email    string `json:"email"`
	Name     string `json:"name"`
	Password string `json:"password"`
	Admin    bool   `json:"admin"`
}

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

// decodeAdminUserInput decode le JSON d'entree pour user admin.
//
// @param httpRequest *http.Request Requete HTTP entrante
// @return *adminUserInput Input decode ou nil si erreur
// @return error Erreur de decodage
func decodeAdminUserInput(httpRequest *http.Request) (*adminUserInput, error) {
	var userInput adminUserInput
	decodeError := json.NewDecoder(httpRequest.Body).Decode(&userInput)
	if decodeError != nil {
		return nil, decodeError
	}
	return &userInput, nil
}

// hashPassword genere le hash bcrypt du mot de passe.
//
// @param clearPassword string Mot de passe en clair
// @return string Hash bcrypt
func hashPassword(clearPassword string) string {
	passwordHash, _ := bcrypt.GenerateFromPassword([]byte(clearPassword), 10)
	return string(passwordHash)
}

// handleDuplicateEmail gere l'erreur de duplication email.
//
// @param responseWriter http.ResponseWriter Writer HTTP
// @param dbError error Erreur a analyser
func handleDuplicateEmail(responseWriter http.ResponseWriter, dbError error) {
	if strings.Contains(dbError.Error(), "duplicate key") {
		http.Error(responseWriter, "email exists", http.StatusConflict)
	} else {
		http.Error(responseWriter, "failed to create user", http.StatusInternalServerError)
	}
}

// sendUserResponse envoie la reponse JSON user sans mot de passe.
//
// @param responseWriter http.ResponseWriter Writer HTTP
// @param targetUser *models.User Utilisateur a envoyer
// @param httpStatus int Code HTTP
func sendUserResponse(responseWriter http.ResponseWriter, targetUser *models.User, httpStatus int) {
	if httpStatus != 0 {
		responseWriter.WriteHeader(httpStatus)
	}
	targetUser.Password = ""
	json.NewEncoder(responseWriter).Encode(targetUser)
}

// ==============================================================================
// Handlers admin
// ==============================================================================

// AdminListUsers liste tous les utilisateurs (route admin).
//
// @param responseWriter http.ResponseWriter Writer de reponse HTTP
// @param httpRequest *http.Request Requete HTTP entrante
func AdminListUsers(responseWriter http.ResponseWriter, httpRequest *http.Request) {
	var userList []models.User
	db.Connect().Select("id", "created_at", "updated_at", "email", "name", "admin").Order("admin DESC, name ASC").Find(&userList)
	if userList == nil {
		userList = make([]models.User, 0)
	}
	responseWriter.Header().Set("Content-Type", "application/json")
	json.NewEncoder(responseWriter).Encode(userList)
}

// AdminCreateUser cree un utilisateur (route admin).
//
// @param responseWriter http.ResponseWriter Writer de reponse HTTP
// @param httpRequest *http.Request Requete HTTP entrante
func AdminCreateUser(responseWriter http.ResponseWriter, httpRequest *http.Request) {
	userInput, decodeError := decodeAdminUserInput(httpRequest)
	if decodeError != nil {
		http.Error(responseWriter, "bad request", 400)
		return
	}
	newUser := models.User{Email: userInput.Email, Name: userInput.Name, Password: hashPassword(userInput.Password), Admin: userInput.Admin}
	if createError := db.Connect().Create(&newUser).Error; createError != nil {
		handleDuplicateEmail(responseWriter, createError)
		return
	}
	sendUserResponse(responseWriter, &newUser, http.StatusCreated)
}

// AdminUpdateUser met a jour un utilisateur (route admin).
//
// @param responseWriter http.ResponseWriter Writer de reponse HTTP
// @param httpRequest *http.Request Requete HTTP entrante
func AdminUpdateUser(responseWriter http.ResponseWriter, httpRequest *http.Request) {
	pathVars := mux.Vars(httpRequest)
	userID, _ := strconv.Atoi(pathVars["id"])
	var updateData map[string]any
	if decodeError := json.NewDecoder(httpRequest.Body).Decode(&updateData); decodeError != nil {
		http.Error(responseWriter, "invalid json", http.StatusBadRequest)
		return
	}
	gormDB := db.Connect()
	if updateError := gormDB.Model(&models.User{}).Where("id = ?", userID).Updates(updateData).Error; updateError != nil {
		http.Error(responseWriter, "update failed", http.StatusInternalServerError)
		return
	}
	var updatedUser models.User
	gormDB.First(&updatedUser, userID)
	sendUserResponse(responseWriter, &updatedUser, 0)
}

// AdminDeleteUser supprime un utilisateur (route admin).
//
// @param responseWriter http.ResponseWriter Writer de reponse HTTP
// @param httpRequest *http.Request Requete HTTP entrante
func AdminDeleteUser(responseWriter http.ResponseWriter, httpRequest *http.Request) {
	pathVars := mux.Vars(httpRequest)
	userID, _ := strconv.Atoi(pathVars["id"])
	db.Connect().Unscoped().Delete(&models.User{}, userID)
	responseWriter.WriteHeader(http.StatusOK)
}
