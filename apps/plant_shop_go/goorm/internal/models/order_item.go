package models

import "gorm.io/gorm"

type OrderItem struct {
	gorm.Model
	OrderID uint
	PlantID uint
	Quantity int
}
