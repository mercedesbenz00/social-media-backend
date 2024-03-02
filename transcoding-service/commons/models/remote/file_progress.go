package remote

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/metadata"
)

type FileProgressDto struct {
	models.BaseModel
	Status    string                  `json:"status"`
	DoneFiles []models.TranscodedFile `json:"done_files"`
	//TODO later
	//UnDoneFiles []TemplateProgressDto   `json:"un_done_files"`
}

type TemplateProgressDto struct {
	models.BaseModel
	url      string                 `json:"url"`
	metadata metadata.MediaMetaData `json:"metadata"`
	status   string                 `json:"status"`
}

func NewTemplateProgressDto(baseModel models.BaseModel, url string, metadata metadata.MediaMetaData, status string) *TemplateProgressDto {
	return &TemplateProgressDto{BaseModel: baseModel, url: url, metadata: metadata, status: status}
}
