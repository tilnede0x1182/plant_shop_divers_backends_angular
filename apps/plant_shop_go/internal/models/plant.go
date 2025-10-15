package models

import (
	"time"

	"gorm.io/gorm"
)

type Plant struct {
	ID          uint           `gorm:"primaryKey" json:"id"`
	CreatedAt   time.Time      `json:"createdAt"`
	UpdatedAt   time.Time      `json:"updatedAt"`
	DeletedAt   gorm.DeletedAt `gorm:"index" json:"-"`
	Name        string         `gorm:"not null" json:"name"`
	Price       float64        `gorm:"not null" json:"price"`
	Description string         `json:"description"`
	Stock       int            `gorm:"not null;default:0" json:"stock"`
}
