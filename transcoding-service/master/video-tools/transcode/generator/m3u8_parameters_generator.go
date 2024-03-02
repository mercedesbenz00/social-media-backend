package generator

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models/remote"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/metadata"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/transcode/processor"
	"fmt"
	"strconv"
)

type M3U8ParametersGenerator struct {
	TranscodeTemplate  *models.TranscodeTemplate
	ChunksOutputFolder string
	Processors         []processor.TemplateProcessor
}

var m3U8ValueNamePairs = [][]string{
	{"-i", "input_flag"},
	{SPECIAL_CHAR, "input_value"},
	{"-hls_list_size", "playlist_limit"},
	{SPECIAL_CHAR, "playlist_limit_value"},
	{"-ss", "seek_flag"},
	{SPECIAL_CHAR, "start_time"},
	{"-t", "time_flag"},
	{SPECIAL_CHAR, "end_time"},
	{"-c:v", "video_encoder_flag"},
	{"libx264", "video_encoder_value"},
	{"-hls_init_time", "hls_time"},
	{strconv.Itoa(models.MINIMUM_CHUNK_SIZE), "hls_time_value"},
	//{"-maxrate", "maxrate"},
	//{"6750k", "maxrate_value"},
	//{"-bufsize", "bufsize"},
	//{"6750k", "bufsize_value"},
	{"-preset", "preset_flag"},
	{SPECIAL_CHAR, "preset_value"},
	{"-vf", "size_flag"},
	{SPECIAL_CHAR, "size_value"},
	{"-crf", "resolution_flag"},
	{SPECIAL_CHAR, "resolution_value"},
	{SPECIAL_CHAR, "output_value"},
}

func (t *M3U8ParametersGenerator) UpdateTranscodeProfile(profile *models.TranscodeTemplate) {
	t.TranscodeTemplate = profile
}

func (t *M3U8ParametersGenerator) ApplyTranscodingPreprocessors(f *models.TranscodingFile, m metadata.MediaMetaData) {
	for _, p := range t.Processors {
		p.Process(f, m, t.TranscodeTemplate)
	}
}

func (t *M3U8ParametersGenerator) FillProfileParameters(chunk remote.FileChunkDto) []models.ChunkTranscodeParameter {

	chunkProfile := chunk.TranscodeProfile
	chunkProfile.ChunkTranscodeParameters = t.getParametersTemplate(chunk)
	parameters := chunkProfile.ChunkTranscodeParameters

	for i, _ := range parameters {
		parameterName := parameters[i].Name
		if parameters[i].Value == SPECIAL_CHAR {
			if parameterName == "start_time" {
				parameters[i].Value = fmt.Sprintf("%f", chunk.TimeSplitInformation.StartTime)
			} else if parameterName == "end_time" {
				timeDifference := chunk.TimeSplitInformation.EndTime - chunk.TimeSplitInformation.StartTime
				parameters[i].Value = fmt.Sprintf("%f", timeDifference)
			} else if parameterName == "input_value" {
				parameters[i].Value = chunk.StorageProfile.Url
			} else if parameterName == "output_value" {
				fileName := chunk.ID + "." + chunkProfile.OutputFileExtension
				parameters[i].Value = helper.GetFullFilePath(t.ChunksOutputFolder, chunk.TranscodingFileID, chunk.ID, fileName)
			} else if parameterName == "resolution_value" {
				parameters[i].Value = t.getValueForGivenName(parameterName, "23")
			} else if parameterName == "size_value" {
				parameters[i].Value = t.getValueForGivenName(parameterName, "scale=320:-2")
			} else if parameterName == "preset_value" {
				parameters[i].Value = t.getValueForGivenName(parameterName, "medium")
			} else if parameterName == "playlist_limit_value" {
				parameters[i].Value = t.getValueForGivenName(parameterName, "0")
			}
		}
	}

	return parameters
}

func (t *M3U8ParametersGenerator) getParametersTemplate(chunk remote.FileChunkDto) []models.ChunkTranscodeParameter {
	parameters := []models.ChunkTranscodeParameter{}

	for _, valueNamePair := range m3U8ValueNamePairs {
		parameters = t.addParameter(parameters, valueNamePair[0], valueNamePair[1], chunk)
	}

	return parameters
}

func (t *M3U8ParametersGenerator) addParameter(parameters []models.ChunkTranscodeParameter, value string, name string, chunk remote.FileChunkDto) []models.ChunkTranscodeParameter {
	return append(parameters, models.ChunkTranscodeParameter{models.BaseModel{ID: helper.GenerateUUID()},
		models.BaseTranscodeParameter{
			Value: value,
			Name:  name,
		},
		chunk.TranscodeProfile.ID,
	})
}

func (t *M3U8ParametersGenerator) getValueForGivenName(n string, defaultValue string) string {
	for _, p := range t.TranscodeTemplate.FileTranscodeParameters {
		if p.Name == n {
			return p.Value
		}
	}
	return defaultValue
}
