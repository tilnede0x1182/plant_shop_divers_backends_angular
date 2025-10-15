package security

import (
	"time"
	"os"

	"github.com/golang-jwt/jwt/v5"
)

/*
Génère et vérifie des JWT HMAC pour cookie httpOnly "ps_token"
@userID identifiant utilisateur
@duration durée de validité
*/
type Claims struct {
	UserID string `json:"uid"`
	jwt.RegisteredClaims
}

func secret() []byte {
	key := os.Getenv("JWT_SECRET")
	if key == "" {
		key = "dev-insecure" // à remplacer en prod
	}
	return []byte(key)
}

func GenerateToken(userID string, duration time.Duration) (string, error) {
	claims := &Claims{
		UserID: userID,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(duration)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(secret())
}

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
