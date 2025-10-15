package models

import (
	"time"

	"gorm.io/gorm"
)

type OrderItem struct {
	ID        uint           `gorm:"primaryKey" json:"id"`
	CreatedAt time.Time      `json:"createdAt"`
	UpdatedAt time.Time      `json:"updatedAt"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
	OrderID   uint           `json:"orderId"`
	PlantID   uint           `json:"plantId"`
	Quantity  int            `json:"quantity"`
	Plant     Plant          `json:"plant"`
}
