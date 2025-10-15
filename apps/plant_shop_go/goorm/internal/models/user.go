package models

import "gorm.io/gorm"

type User struct {
	gorm.Model
	Email    string `gorm:"unique;not null"`
	Password string `gorm:"not null"`
	Name     string
	Admin    bool   `gorm:"default:false"`
	Orders   []Order
}
