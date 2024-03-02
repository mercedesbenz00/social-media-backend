package models

import "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"

type Error struct {
	BaseModel
	Cause string
	Code  string
}

type FileError struct {
	Error
	FileID    string
	ProfileID string
}

func NewFileError(fileID, profileID, cause, code string) *FileError {
	return &FileError{Error: Error{
		BaseModel: BaseModel{ID: helper.GenerateUUID()},
		Cause:     cause,
		Code:      code,
	}, FileID: fileID, ProfileID: profileID}
}

type ChunkError struct {
	Error
	ChunkID string
}

func NewChunkError(chunkID, cause, code string) *ChunkError {
	return &ChunkError{Error: Error{
		BaseModel: BaseModel{ID: helper.GenerateUUID()},
		Cause:     cause,
		Code:      code,
	}, ChunkID: chunkID}
}
