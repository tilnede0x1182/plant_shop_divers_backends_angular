package models

import "gorm.io/gorm"

type Plant struct {
	gorm.Model
	Name        string  `gorm:"not null"`
	Price       float64 `gorm:"not null"`
	Description string
	Stock       int     `gorm:"not null;default:0"`
}
