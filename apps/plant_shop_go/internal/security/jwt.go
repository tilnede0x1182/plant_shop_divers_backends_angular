package security

import (
	"net/http"
	"os"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

/*
Gestion simple des JWT HMAC et du cookie httpOnly "ps_token".
Claims expose UserID et Admin (bool) utilisés par les middlewares.
*/

type Claims struct {
	UserID string `json:"uid"`
	Admin  bool   `json:"admin"`
	jwt.RegisteredClaims
}

// secret retourne la cle secrete JWT depuis l environnement.
//
// @return []byte Cle secrete pour signer les tokens
func secret() []byte {
	key := os.Getenv("JWT_SECRET")
	if key == "" {
		// Valeur de secours pour dev ; en production définir JWT_SECRET.
		key = "dev-insecure"
	}
	return []byte(key)
}

// GenerateToken genere un JWT signe contenant uid, le role admin et la duree.
//
// @param userID string ID de l utilisateur
// @param admin bool Role administrateur
// @param duration time.Duration Duree de validite du token
// @return string Token JWT signe
// @return error Erreur eventuelle
func GenerateToken(userID string, admin bool, duration time.Duration) (string, error) {
	claims := &Claims{
		UserID: userID,
		Admin:  admin,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(duration)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(secret())
}

// ParseToken verifie le JWT et retourne les claims.
//
// @param tokenString string Token JWT a verifier
// @return *Claims Claims extraits du token
// @return error Erreur eventuelle
func ParseToken(tokenString string) (*Claims, error) {
	token, err := jwt.ParseWithClaims(tokenString, &Claims{}, func(token *jwt.Token) (interface{}, error) {
		return secret(), nil
	})
	if err != nil {
		return nil, err
	}
	if claims, ok := token.Claims.(*Claims); ok && token.Valid {
		return claims, nil
	}
	return nil, jwt.ErrTokenInvalidClaims
}

// SetCookie ecrit le cookie httpOnly ps_token sur la reponse.
//
// @param w http.ResponseWriter Writer de reponse HTTP
// @param token string Token JWT a stocker
func SetCookie(w http.ResponseWriter, token string) {
	http.SetCookie(w, &http.Cookie{
		Name:     "ps_token",
		Value:    token,
		Path:     "/",
		HttpOnly: true,
		SameSite: http.SameSiteLaxMode,
		Secure:   false, // Mettre à true en production (HTTPS)
	})
}

// ClearCookie supprime le cookie ps_token.
//
// @param w http.ResponseWriter Writer de reponse HTTP
func ClearCookie(w http.ResponseWriter) {
	http.SetCookie(w, &http.Cookie{
		Name:     "ps_token",
		Value:    "",
		Path:     "/",
		HttpOnly: true,
		SameSite: http.SameSiteLaxMode,
		MaxAge:   -1,
		Secure:   false, // Mettre à true en production (HTTPS)
	})
}
