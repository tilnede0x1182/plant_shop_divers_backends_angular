package models

// ==============================================================================
// Importations
// ==============================================================================

import (
	"time"

	"gorm.io/gorm"
)

// ==============================================================================
// Types
// ==============================================================================

// User represente un utilisateur du systeme.
type User struct {
	ID        uint           `gorm:"primaryKey" json:"id"`
	CreatedAt time.Time      `json:"createdAt"`
	UpdatedAt time.Time      `json:"updatedAt"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"` // Caché du JSON
	Email     string         `gorm:"unique;not null" json:"email"`
	Password  string         `gorm:"not null" json:"-"` // Caché du JSON
	Name      string         `json:"name"`
	Admin     bool           `gorm:"default:false" json:"admin"`
	Orders    []Order        `json:"orders,omitempty"`
}
