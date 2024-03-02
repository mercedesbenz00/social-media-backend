package db

import (
	. "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"github.com/jinzhu/gorm"
	"time"
)

type FilesDao struct {
	Db *gorm.DB
}

func (dao *FilesDao) GetFile(id string) TranscodingFile {
	file := TranscodingFile{}
	dao.Db.Model(&TranscodingFile{}).Where("id = ? ", id).Find(&file)
	return file
}

func (dao *FilesDao) InsertFile(file TranscodingFile) {
	dao.Db.Create(&file)
}

func (dao *FilesDao) UpdateDuration(fileID string, value float64) {
	dao.Db.Model(&TranscodingFile{BaseModel: BaseModel{ID: fileID}}).UpdateColumn("duration", value)
}

func (dao *FilesDao) UpdateM3U8Link(fileID string, value string) {
	dao.Db.Model(&TranscodingFile{BaseModel: BaseModel{ID: fileID}}).UpdateColumn("m3_u8_url", value)
}

func (dao *FilesDao) GetCallbackFiles() []TranscodingFile {
	var files []TranscodingFile

	now := time.Now()
	dao.Db.Model(&TranscodingFile{}).
		Where("callback_failed = ?", false).
		Where("callback_next_call < ?", now).Find(&files)

	return files
}

func (dao *FilesDao) UpdateFileStatus(fileID string, value string) {
	dao.Db.Model(&TranscodingFile{BaseModel: BaseModel{ID: fileID}}).UpdateColumn("file_status", value)
}

func (dao *FilesDao) ScheduleCallback(fileID string, callbackCounter int) bool {
	update := map[string]interface{}{
		"callback_counter": callbackCounter,
	}
	callbackNextCall := GetNextCallback(callbackCounter)
	if callbackNextCall.IsZero() {
		update["callback_failed"] = true
	} else {
		update["callback_next_call"] = callbackNextCall
	}
	dao.Db.Model(&TranscodingFile{BaseModel: BaseModel{ID: fileID}}).Updates(update)
	return !callbackNextCall.IsZero()
}

func (dao *FilesDao) SuccessCallback(fileID string) {
	dao.Db.Model(&TranscodingFile{BaseModel: BaseModel{ID: fileID}}).Update("callback_next_call", nil)
}

func (dao *FilesDao) FailCallback(fileID string) {
	dao.Db.Model(&TranscodingFile{BaseModel: BaseModel{ID: fileID}}).Update("callback_failed", true)
}

func (dao *FilesDao) InsertTranscodedFile(file TranscodedFile) {
	dao.Db.Create(&file)
}

func (dao *FilesDao) GetTranscodedFilesForSourceId(id string) []TranscodedFile {
	files := []TranscodedFile{}
	dao.Db.Model(&TranscodedFile{}).Where("original_file_id = ? ", id).Preload("TranscodedFileMetaData").Find(&files)
	return files
}
