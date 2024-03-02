package db

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"github.com/jinzhu/gorm"
)

type ErrorDao struct {
	Db *gorm.DB
}

func (dao *ErrorDao) InsertChunkError(e models.ChunkError) {
	dao.Db.Create(&e)
}

func (dao *ErrorDao) InsertFileError(e models.FileError) {
	dao.Db.Create(&e)
}
