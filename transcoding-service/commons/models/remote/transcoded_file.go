package remote

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/metadata"
)

type TranscodedFileDto struct {
	models.BaseModel
	PostID     int                    `json:"post_id"`
	Resolution string                 `json:"resolution"`
	Url        string                 `json:"storage_location"`
	MetaData   metadata.MediaMetaData `json:"meta_data"`
}

func NewTranscodedFile(id string, postId int, resolution string, url string, metaData metadata.MediaMetaData) *TranscodedFileDto {
	return &TranscodedFileDto{BaseModel: models.BaseModel{ID: id}, PostID: postId, Resolution: resolution, Url: url, MetaData: metaData}
}
