package processor

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/metadata"
	"strconv"
)

type VerticalSizeProcessor struct{}

func (s *VerticalSizeProcessor) Process(f *models.TranscodingFile, m metadata.MediaMetaData, template *models.TranscodeTemplate) {
	viableResolutions := s.GetTheAvailableSizesForThisVideo(m.Height)

	for _, res := range viableResolutions {
		profileCopy := *template
		profileCopy.ID = helper.GenerateUUID()

		profileCopy.SetParameter(helper.GenerateUUID(), "size_value", "scale=-2:"+strconv.Itoa(res))
		profileCopy.Variation = models.VERTICAL_RESOLUTIONS[res]
		f.TranscodeTemplates = append(f.TranscodeTemplates, profileCopy)
	}
}

func (s *VerticalSizeProcessor) GetTheAvailableSizesForThisVideo(height int) []int {

	var res []int
	for verRes := range models.VERTICAL_RESOLUTIONS {
		if height >= verRes {
			res = append(res, verRes)
		}
	}
	return res
}
