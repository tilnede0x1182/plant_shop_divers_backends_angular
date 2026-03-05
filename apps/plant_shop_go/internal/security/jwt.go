package security

// ==============================================================================
// Importations
// ==============================================================================

import (
	"net/http"
	"os"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

// ==============================================================================
// Types
// ==============================================================================

/*
Gestion simple des JWT HMAC et du cookie httpOnly "ps_token".
Claims expose UserID et Admin (bool) utilisés par les middlewares.
*/

type Claims struct {
	UserID string `json:"uid"`
	Admin  bool   `json:"admin"`
	jwt.RegisteredClaims
}

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

// secret retourne la cle secrete JWT depuis l environnement.
//
// @return []byte Cle secrete pour signer les tokens
func secret() []byte {
	secretKey := os.Getenv("JWT_SECRET")
	if secretKey == "" {
		// Valeur de secours pour dev ; en production définir JWT_SECRET.
		secretKey = "dev-insecure"
	}
	return []byte(secretKey)
}

// GenerateToken genere un JWT signe contenant uid, le role admin et la duree.
//
// @param userID string ID de l utilisateur
// @param admin bool Role administrateur
// @param duration time.Duration Duree de validite du token
// @return string Token JWT signe
// @return error Erreur eventuelle
func GenerateToken(userID string, isAdmin bool, tokenDuration time.Duration) (string, error) {
	tokenClaims := &Claims{
		UserID: userID,
		Admin:  isAdmin,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(tokenDuration)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	}
	jwtToken := jwt.NewWithClaims(jwt.SigningMethodHS256, tokenClaims)
	return jwtToken.SignedString(secret())
}

// ParseToken verifie le JWT et retourne les claims.
//
// @param tokenString string Token JWT a verifier
// @return *Claims Claims extraits du token
// @return error Erreur eventuelle
func ParseToken(jwtString string) (*Claims, error) {
	parsedToken, parseError := jwt.ParseWithClaims(jwtString, &Claims{}, func(jwtToken *jwt.Token) (interface{}, error) {
		return secret(), nil
	})
	if parseError != nil {
		return nil, parseError
	}
	if tokenClaims, claimsValid := parsedToken.Claims.(*Claims); claimsValid && parsedToken.Valid {
		return tokenClaims, nil
	}
	return nil, jwt.ErrTokenInvalidClaims
}

// SetCookie ecrit le cookie httpOnly ps_token sur la reponse.
//
// @param responseWriter http.ResponseWriter Writer de reponse HTTP
// @param jwtToken string Token JWT a stocker
func SetCookie(responseWriter http.ResponseWriter, jwtToken string) {
	http.SetCookie(responseWriter, &http.Cookie{
		Name:     "ps_token",
		Value:    jwtToken,
		Path:     "/",
		HttpOnly: true,
		SameSite: http.SameSiteLaxMode,
		Secure:   false, // Mettre à true en production (HTTPS)
	})
}

// ClearCookie supprime le cookie ps_token.
//
// @param responseWriter http.ResponseWriter Writer de reponse HTTP
func ClearCookie(responseWriter http.ResponseWriter) {
	http.SetCookie(responseWriter, &http.Cookie{
		Name:     "ps_token",
		Value:    "",
		Path:     "/",
		HttpOnly: true,
		SameSite: http.SameSiteLaxMode,
		MaxAge:   -1,
		Secure:   false, // Mettre à true en production (HTTPS)
	})
}
