package models

type FileChunk struct {
	BaseModel
	TranscodeProfile     ChunkTranscodeProfile `json:"transcode_profile"`
	PostID               int                   `json:"post_id"`
	StorageProfileID     string                `json:"storage_profile_id"`
	TranscodingFileID    string                `json:"transcoding_file_id"`
	ChunkStatus          string                `json:"chunk_status"`
	TimeSplitInformation TimeSplitInformation  `json:"split_information" gorm:"foreignkey:file_chunk_id"`
	MasterNode           string                `json:"master_node"`
	OperatingNodeName    string
}
