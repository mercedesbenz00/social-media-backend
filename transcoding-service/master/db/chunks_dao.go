package db

import (
	. "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"github.com/jinzhu/gorm"
	"github.com/thoas/go-funk"
	"sort"
)

type ChunksDao struct {
	Db *gorm.DB
}

func (dao *ChunksDao) InsertChunk(chunk FileChunk) {
	dao.Db.Create(&chunk)
}

func (dao *ChunksDao) GetChunks(fileID string) []FileChunk {
	chunks := []FileChunk{}
	dao.Db.Model(&FileChunk{}).Where("transcoding_file_id = ? ", fileID).Preload("TranscodeProfile").Find(&chunks)
	return chunks
}

func (dao *ChunksDao) UpdateChunkStatus(chunkID string, value string) {
	dao.Db.Model(&FileChunk{BaseModel: BaseModel{ID: chunkID}}).UpdateColumn("chunk_status", value)
}

func (dao *ChunksDao) GetUndoneChunksCountForProfile(fileID string, profileID string) int {
	chunks := []FileChunk{}
	dao.Db.Model(&FileChunk{}).Where("transcoding_file_id = ? AND chunk_status != ?", fileID, CHUNK_DONE).Preload("TranscodeProfile").Find(&chunks)
	return len(getChunksWithProfileID(chunks, profileID))
}

func (dao *ChunksDao) GetUndoneChunksCount(fileID string) int {
	chunks := []FileChunk{}
	dao.Db.Model(&FileChunk{}).Where("transcoding_file_id = ? AND chunk_status != ?", fileID, CHUNK_DONE).Preload("TranscodeProfile").Find(&chunks)
	return len(chunks)
}

func (dao *ChunksDao) GetDoneChunksForFile(fileID string, profileID string) []FileChunk {
	chunks := []FileChunk{}
	dao.Db.Where("transcoding_file_id = ? AND  chunk_status = ?", fileID, CHUNK_DONE).Preload("TranscodeProfile").Preload("TimeSplitInformation").Find(&chunks)
	sort.SliceStable(chunks, func(i, j int) bool {
		return chunks[i].TimeSplitInformation.StartTime < chunks[j].TimeSplitInformation.StartTime
	})
	return getChunksWithProfileID(chunks, profileID)
}

func (dao *ChunksDao) GetDoneChunksForFileForSpecificEncoder(fileID string, encoder string) [][]FileChunk {
	chunks := []FileChunk{}
	profiles := []TranscodeTemplate{}
	chunksGroups := [][]FileChunk{}

	dao.Db.Where("transcoding_file_id = ? AND  chunk_status = ?", fileID, CHUNK_DONE).Preload("TranscodeProfile").Preload("TimeSplitInformation").Find(&chunks)
	sort.SliceStable(chunks, func(i, j int) bool {
		return chunks[i].TimeSplitInformation.StartTime < chunks[j].TimeSplitInformation.StartTime
	})

	dao.Db.Where("transcoding_file_id = ? AND  encoder = ?", fileID, encoder).Find(&profiles)

	for _, p := range profiles {
		cs := getChunksWithProfileID(chunks, p.ID)
		chunksGroups = append(chunksGroups, cs)
	}

	return chunksGroups
}

func getChunksWithProfileID(chunks []FileChunk, profileID string) []FileChunk {
	result := funk.Filter(chunks, func(c FileChunk) bool {
		return c.TranscodeProfile.TranscodeTemplateID == profileID
	})
	switch result := result.(type) {
	case []FileChunk:
		return result
	}
	return nil
}
