package processor

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/metadata"
	"strconv"
)

type SizeProcessor struct{}

func (s *SizeProcessor) Process(f *models.TranscodingFile, m metadata.MediaMetaData, template *models.TranscodeTemplate) {
	numOfPixels := m.Height * m.Width
	viableResolutions := getTheAvailableSizesForThisVideo(numOfPixels)

	for _, res := range viableResolutions {
		var profileCopy models.TranscodeTemplate
		profileCopy = *template
		profileCopy.ID = helper.GenerateUUID()

		profileCopy.SetParameter(helper.GenerateUUID(), "size_value", "scale="+strconv.Itoa(res)+":-2")
		f.TranscodeTemplates = append(f.TranscodeTemplates, profileCopy)
	}
}

func getTheAvailableSizesForThisVideo(numbOfPixelsForSource int) []int {
	availableResolutions := [5]int{426, 640, 854, 1280, 1920}
	for i, res := range availableResolutions {
		resolutionPixels := res * res * 9 / 16

		//allow 10% error tolerance in the video size
		errorTolerance := resolutionPixels / 10
		if (resolutionPixels - errorTolerance) > numbOfPixelsForSource {
			return availableResolutions[:i]
		}
	}
	return availableResolutions[:]
}
