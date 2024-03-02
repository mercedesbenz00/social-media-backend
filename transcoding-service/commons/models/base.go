package models

import "time"

type BaseModel struct {
	ID        string     `json:"id" gorm:"primary_key;default:''"`
	CreatedAt time.Time  `json:"-"`
	UpdatedAt time.Time  `json:"-"`
	DeletedAt *time.Time `sql:"index" json:"-"`
}
