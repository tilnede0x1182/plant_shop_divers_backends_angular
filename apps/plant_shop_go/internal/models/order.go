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

// Order represente une commande client.
type Order struct {
	ID         uint           `gorm:"primaryKey" json:"id"`
	CreatedAt  time.Time      `json:"createdAt"`
	UpdatedAt  time.Time      `json:"updatedAt"`
	DeletedAt  gorm.DeletedAt `gorm:"index" json:"-"`
	UserID     uint           `json:"userId"`
	TotalPrice float64        `json:"totalPrice"`
	Status     string         `json:"status"`
	Items      []OrderItem    `json:"orderItems,omitempty"`
}
