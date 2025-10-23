package db

import (
	"log"
	"os"
	"sync"
	"time"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

var (
	conn     *gorm.DB
	connOnce sync.Once
)

// Connect renvoie toujours le même pool GORM et limite les connexions.
func Connect() *gorm.DB {
	connOnce.Do(func() {
		dsn := os.Getenv("DATABASE_URL")
		db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{})
		if err != nil {
			log.Fatalf("DB connection failed: %v", err)
		}
		sqlDB, _ := db.DB()
		sqlDB.SetMaxOpenConns(10)
		sqlDB.SetMaxIdleConns(5)
		sqlDB.SetConnMaxLifetime(time.Hour)
		conn = db
	})
	return conn
}
