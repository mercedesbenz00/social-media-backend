package generator

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models/remote"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/metadata"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/transcode/processor"
)

type ParametersGenerator interface {
	FillProfileParameters(chunk remote.FileChunkDto) []models.ChunkTranscodeParameter
	ApplyTranscodingPreprocessors(f *models.TranscodingFile, m metadata.MediaMetaData)
	UpdateTranscodeProfile(profile *models.TranscodeTemplate)
}

func NewTranscoderGenerator(nodeTmpFolder string, transcodeTemplate *models.TranscodeTemplate) ParametersGenerator {

	if transcodeTemplate.Encoder == models.DEFAULT_ENCODER || transcodeTemplate.Encoder == models.ADAPTIVE_ENCODER {
		generator := &H264ParametersGenerator{
			ChunksOutputFolder: nodeTmpFolder,
			TranscodeTemplate:  transcodeTemplate,
		}
		generator.Processors = append(generator.Processors, &processor.SizeProcessor{})
		return generator
	} else {
		generator := &M3U8ParametersGenerator{
			ChunksOutputFolder: nodeTmpFolder,
			TranscodeTemplate:  transcodeTemplate,
		}
		generator.Processors = append(generator.Processors, &processor.VerticalSizeProcessor{})
		return generator
	}
	return nil
}
