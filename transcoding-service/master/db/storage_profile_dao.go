package db

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"github.com/jinzhu/gorm"
)

type StorageProfileDao struct {
	Db *gorm.DB
}

func (dao *StorageProfileDao) GetProfile(id string) models.StorageProfile {
	profile := models.StorageProfile{}
	dao.Db.Where("id = ?", id).Preload("StorageLocations").First(&profile)
	return profile
}
