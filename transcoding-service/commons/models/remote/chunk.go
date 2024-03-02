package remote

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"
	. "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
)

type FileChunkDto struct {
	BaseModel
	TranscodeProfile     ChunkTranscodeProfile `json:"transcode_profile"`
	StorageProfile       StorageProfile        `json:"storage_profile"`
	PostID               int                   `json:"post_id"`
	TranscodingFileID    string                `json:"transcoding_file_id"`
	ChunkStatus          string                `json:"chunk_status"`
	TimeSplitInformation TimeSplitInformation  `json:"split_information"`
	MasterNode           string                `json:"master_node"`
	Error                ChunkError            `json:"error"`
}

func (c *FileChunkDto) ToLocalChunk() FileChunk {
	chunk := FileChunk{
		BaseModel:            BaseModel{ID: c.ID},
		TranscodeProfile:     c.TranscodeProfile,
		PostID:               c.PostID,
		StorageProfileID:     c.StorageProfile.ID,
		TranscodingFileID:    c.TranscodingFileID,
		ChunkStatus:          c.ChunkStatus,
		TimeSplitInformation: c.TimeSplitInformation,
		MasterNode:           c.MasterNode,
	}
	chunk.TimeSplitInformation.ID = helper.GenerateUUID()
	return chunk
}

func (c *FileChunkDto) SetProfiles(chunkProfile *ChunkTranscodeProfile, profile *StorageProfile) {
	c.StorageProfile = *profile
	c.TranscodeProfile = *chunkProfile
}
