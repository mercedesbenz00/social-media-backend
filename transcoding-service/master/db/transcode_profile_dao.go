package db

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"github.com/jinzhu/gorm"
)

type TranscodeProfileDao struct {
	Db *gorm.DB
}

func (dao *TranscodeProfileDao) InsertProfileForChunk(profile models.ChunkTranscodeProfile) {
	dao.Db.Create(&profile)
}

func (dao *TranscodeProfileDao) GetProfileForChunk(id string) models.ChunkTranscodeProfile {
	profile := models.ChunkTranscodeProfile{}
	dao.Db.Where("id = ?", id).First(&profile)
	return profile
}

type TranscodeTemplateDao struct {
	Db *gorm.DB
}

func (dao *TranscodeTemplateDao) InsertTranscodeTemplate(templates []models.TranscodeTemplate) {
	for _, t := range templates {
		dao.Db.Create(&t)
	}
}

func (dao *TranscodeTemplateDao) DeleteTranscodeTemplate(t models.TranscodeTemplate) {
	dao.Db.Delete(&t)
}
