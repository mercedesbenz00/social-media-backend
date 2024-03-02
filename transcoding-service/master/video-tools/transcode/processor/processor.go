package processor

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/metadata"
)

type TemplateProcessor interface {
	Process(f *models.TranscodingFile, m metadata.MediaMetaData, profile *models.TranscodeTemplate)
}
