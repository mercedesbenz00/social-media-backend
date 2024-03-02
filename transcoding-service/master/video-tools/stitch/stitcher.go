package stitch

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models/remote"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/db"
)

type Stitcher interface {
	StitchIfPartsDone(chunk remote.FileChunkDto) error
	arePartsDone(chunk remote.FileChunkDto) bool
	Stitch(chunk remote.FileChunkDto) error
}

func NewStitcher(h StitchHandle, chunk remote.FileChunkDto) (Stitcher, error) {
	if chunk.TranscodeProfile.Encoder == models.VERTICAL_RESOLUTIONS_ENCODER {
		return &M3U8Stitcher{handle: h}, nil
	} else if chunk.TranscodeProfile.Encoder == models.DEFAULT_ENCODER {
		return &H264Stitcher{handle: h}, nil
	} else if chunk.TranscodeProfile.Encoder == models.ADAPTIVE_ENCODER {
		return &H264AdaptiveStitcher{handle: h}, nil
	}
	return nil, StitchingError{"- Could not find a suitable transcoder type"}
}

type StitchHandle interface {
	GetDbController() *db.Controller
	GetTempFolderLocation() string
	SignUrl(bucket, chunkName string) (string, error)
	UploadAndNotifyServices(chunk remote.FileChunkDto) error
}

type StitchingError struct {
	str string
}

func (s StitchingError) Error() string {
	return s.str
}
