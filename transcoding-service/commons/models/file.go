package models

import (
	"time"

	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/metadata"
)

type TranscodingFile struct {
	BaseModel
	StorageProfile     StorageProfile `json:"storage_profile" gorm:"foreignkey:transcoding_file_id"`
	PostID             int            `json:"post_id"`
	FileStatus         string
	TranscodeTemplates []TranscodeTemplate `json:"transcode_templates" gorm:"foreignkey:transcoding_file_id"`
	Duration           float64             `json:"duration" gorm:"duration"`
	M3U8URL            string              `json:"m3_u8_url" gorm:"m3_u8_url"`
	CallbackURL        string              `json:"callback_url" gorm:"callback_url"`
	CallbackFailed     bool                `gorm:"callback_failed" sql:"index"`
	CallbackCounter    int                 `gorm:"callback_counter"`
	CallbackNextCall   *time.Time          `gorm:"callback_next_call default:null" sql:"index"`
}

type TranscodedFile struct {
	BaseModel
	PostID                 int                    `json:"post_id"`
	OriginalFileID         string                 `json:"original_file_id"`
	OriginalFileTemplateID string                 `json:"original_file_template_id"`
	Url                    string                 `json:"url"`
	TranscodedFileMetaData TranscodedFileMetaData `json:"transcode_templates" gorm:"foreignkey:transcoded_file_id"`
	Variation              string                 `json:"variation" gorm:"variation"`
}

type TranscodedFileMetaData struct {
	BaseModel
	metadata.MediaMetaData
	TranscodedFileID string `json:"transcoded_file_id"`
}
