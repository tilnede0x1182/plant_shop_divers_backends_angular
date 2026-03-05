package handlers

// ==============================================================================
// Importations
// ==============================================================================

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"plant_shop_go/internal/db"
	"plant_shop_go/internal/models"
	"plant_shop_go/internal/security"

	"golang.org/x/crypto/bcrypt"
)

// ==============================================================================
// Types
// ==============================================================================

// authInput structure pour login/register.
type authInput struct {
	Email    string `json:"email"`
	Password string `json:"password"`
	Name     string `json:"name"`
}

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

// decodeAuthInput decode le JSON d'entree auth.
//
// @param httpRequest *http.Request Requete HTTP
// @return *authInput Input decode ou nil
// @return error Erreur de decodage
func decodeAuthInput(httpRequest *http.Request) (*authInput, error) {
	var authData authInput
	if decodeError := json.NewDecoder(httpRequest.Body).Decode(&authData); decodeError != nil {
		return nil, decodeError
	}
	return &authData, nil
}

// emailExists verifie si un email existe deja.
//
// @param emailToCheck string Email a verifier
// @return bool True si existe
func emailExists(emailToCheck string) bool {
	var existingUser models.User
	return db.Connect().Where("email = ?", emailToCheck).First(&existingUser).Error == nil
}

// createUserFromInput cree un user a partir de l'input.
//
// @param authData *authInput Input decode
// @return *models.User User cree
func createUserFromInput(authData *authInput) *models.User {
	passwordHash, _ := bcrypt.GenerateFromPassword([]byte(authData.Password), bcrypt.DefaultCost)
	newUser := models.User{Email: authData.Email, Password: string(passwordHash), Name: authData.Name, Admin: false}
	db.Connect().Create(&newUser)
	return &newUser
}

// setAuthCookie genere et pose le cookie JWT.
//
// @param responseWriter http.ResponseWriter Writer HTTP
// @param targetUserID uint ID utilisateur
// @param isAdmin bool Est admin
func setAuthCookie(responseWriter http.ResponseWriter, targetUserID uint, isAdmin bool) {
	jwtToken, _ := security.GenerateToken(fmt.Sprint(targetUserID), isAdmin, 24*time.Hour)
	security.SetCookie(responseWriter, jwtToken)
}

// ==============================================================================
// Handlers
// ==============================================================================

// Register cree un nouvel utilisateur et retourne un cookie JWT.
//
// @param responseWriter http.ResponseWriter Writer de reponse HTTP
// @param httpRequest *http.Request Requete HTTP entrante
func Register(responseWriter http.ResponseWriter, httpRequest *http.Request) {
	authData, decodeError := decodeAuthInput(httpRequest)
	if decodeError != nil {
		http.Error(responseWriter, "bad request", http.StatusBadRequest)
		return
	}
	if emailExists(authData.Email) {
		http.Error(responseWriter, "email exists", http.StatusConflict)
		return
	}
	createdUser := createUserFromInput(authData)
	setAuthCookie(responseWriter, createdUser.ID, createdUser.Admin)
	responseWriter.WriteHeader(http.StatusCreated)
}

// findUserByEmail cherche un user par email.
//
// @param emailToFind string Email a chercher
// @return *models.User User trouve ou nil
func findUserByEmail(emailToFind string) *models.User {
	var foundUser models.User
	if queryError := db.Connect().Where("email = ?", emailToFind).First(&foundUser).Error; queryError != nil {
		return nil
	}
	return &foundUser
}

// checkPassword verifie le mot de passe.
//
// @param passwordHash string Hash bcrypt
// @param clearPassword string Mot de passe en clair
// @return bool True si valide
func checkPassword(passwordHash string, clearPassword string) bool {
	return bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(clearPassword)) == nil
}

// Login verifie les credentials et retourne un cookie JWT.
//
// @param responseWriter http.ResponseWriter Writer de reponse HTTP
// @param httpRequest *http.Request Requete HTTP entrante
func Login(responseWriter http.ResponseWriter, httpRequest *http.Request) {
	authData, decodeError := decodeAuthInput(httpRequest)
	if decodeError != nil {
		http.Error(responseWriter, "bad request", http.StatusBadRequest)
		return
	}
	foundUser := findUserByEmail(authData.Email)
	if foundUser == nil || !checkPassword(foundUser.Password, authData.Password) {
		http.Error(responseWriter, "invalid creds", http.StatusUnauthorized)
		return
	}
	setAuthCookie(responseWriter, foundUser.ID, foundUser.Admin)
	responseWriter.WriteHeader(http.StatusCreated)
}

// Me retourne les informations de l utilisateur connecte.
//
// @param responseWriter http.ResponseWriter Writer de reponse HTTP
// @param httpRequest *http.Request Requete HTTP entrante
func Me(responseWriter http.ResponseWriter, httpRequest *http.Request) {
	userClaims, claimsFound := httpRequest.Context().Value("claims").(*security.Claims)
	if !claimsFound {
		http.Error(responseWriter, "claims not found in context", http.StatusInternalServerError)
		return
	}
	var currentUser models.User
	db.Connect().First(&currentUser, userClaims.UserID)
	json.NewEncoder(responseWriter).Encode(map[string]any{
		"id": currentUser.ID, "email": currentUser.Email, "name": currentUser.Name, "admin": currentUser.Admin,
	})
}

// Logout supprime le cookie d authentification.
//
// @param responseWriter http.ResponseWriter Writer de reponse HTTP
// @param httpRequest *http.Request Requete HTTP entrante
func Logout(responseWriter http.ResponseWriter, httpRequest *http.Request) {
	security.ClearCookie(responseWriter)
	responseWriter.WriteHeader(http.StatusNoContent)
}
