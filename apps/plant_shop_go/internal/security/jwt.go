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

func secret() []byte {
	key := os.Getenv("JWT_SECRET")
	if key == "" {
		// Valeur de secours pour dev ; en production définir JWT_SECRET.
		key = "dev-insecure"
	}
	return []byte(key)
}

// GenerateToken génère un JWT signé contenant uid, le rôle admin et la durée d'expiration.
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

// ParseToken vérifie le JWT et retourne les claims.
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

// SetCookie écrit le cookie httpOnly "ps_token" sur la réponse.
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

// ClearCookie supprime le cookie "ps_token".
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
