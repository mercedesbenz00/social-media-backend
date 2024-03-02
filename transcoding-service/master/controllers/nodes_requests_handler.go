package controllers

import (
	"bytes"
	"encoding/json"
	"log"
	"path"

	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models/remote"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/metadata"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/stitch"
	"github.com/streadway/amqp"
)

func (c *MasterController) HandleNodeMessage(delivery amqp.Delivery) {

	var b = delivery.Body
	var chunk remote.FileChunkDto
	db := &c.DbController

	err := json.Unmarshal(b, &chunk)
	if err != nil {
		c.Errors <- err
		log.Printf("ERROR: Error while decoding the received message from node %s", err.Error())
		return
	}

	log.Printf("LOG: Master Received a chunk with source id/chunk id/template id: %s/%s/%s", chunk.TranscodeProfile.TranscodingFileID, chunk.ID, chunk.TranscodeProfile.TranscodeTemplateID)

	db.ChunksDao.UpdateChunkStatus(chunk.ID, chunk.ChunkStatus)
	if chunk.ChunkStatus == models.CHUNK_DONE {

		stitcher, err := stitch.NewStitcher(c, chunk)

		if err != nil {
			c.Errors <- err
			log.Printf("ERROR: faile to get stitcher%s", err.Error())
			return
		}

		err = stitcher.StitchIfPartsDone(chunk)
		if err != nil {
			c.Errors <- err
			return
		}

		if db.ChunksDao.GetUndoneChunksCount(chunk.TranscodingFileID) == 0 {
			db.FilesDao.UpdateFileStatus(chunk.TranscodingFileID, models.FILE_DONE)

			// Create M3U8 file
			log.Printf("LOG: Generating m3u8 playlist for %s", chunk.TranscodingFileID)
			m3u8Content := c.GenerateM3U8content(chunk.TranscodingFileID)
			m3u8ContentBytes := []byte(m3u8Content)
			dataSize := len(m3u8ContentBytes)

			storageLocation := chunk.StorageProfile.GetM3U8Location()
			bucketName := storageLocation.Bucket
			m3u8FileName := path.Join(storageLocation.Path, chunk.TranscodingFileID+"."+"m3u8")

			if err = c.StorageController.UploadData(bucketName, m3u8FileName, bytes.NewReader(m3u8ContentBytes), int64(dataSize), models.ACL_PUBLIC_READ); err != nil {
				log.Printf("ERROR: Failed to save m3u8 playlist for %s - %s", chunk.TranscodingFileID, err)
			} else {
				m3u8URL := c.StorageController.GetPublicUrl(bucketName, m3u8FileName)
				db.FilesDao.UpdateM3U8Link(chunk.TranscodingFileID, m3u8URL)
			}

			//Send to rabbitMq
			fileName := helper.GetTranscodedFileName(chunk.TranscodingFileID, chunk.TranscodeProfile.TranscodeTemplateID, chunk.TranscodeProfile.OutputFileExtension)
			meta, _ := metadata.NewMediaMetaData(helper.GetFullFilePath(c.Config.Master.TempFolder, fileName))
			message := remote.NewTranscodedFile(chunk.TranscodingFileID, chunk.PostID, chunk.TranscodeProfile.Variation,
				m3u8FileName, meta)
			jsonMessage, _ := json.Marshal(message)
			c.Bus.SendData(models.POST_VIDEO_TRANSCODED_EVENT, jsonMessage)

			// Schedule callback
			db.FilesDao.ScheduleCallback(chunk.TranscodingFileID, 0)
		}
	} else {
		log.Printf("ERROR: chunk error %s", chunk.Error)
		db.ErrorDao.InsertChunkError(chunk.Error)
	}
}

func (c *MasterController) GetTempFolderLocation() string {
	return c.Config.Master.TempFolder
}

func (c *MasterController) SignUrl(bucket string, chunkName string) (string, error) {
	return c.StorageController.SignUrl(bucket, chunkName)
}

func (c *MasterController) UploadAndNotifyServices(chunk remote.FileChunkDto) error {
	fileName := helper.GetTranscodedFileName(chunk.TranscodingFileID, chunk.TranscodeProfile.TranscodeTemplateID, chunk.TranscodeProfile.OutputFileExtension)
	meta, _ := metadata.NewMediaMetaData(helper.GetFullFilePath(c.Config.Master.TempFolder, fileName))

	// Get tmp file path
	filePath := helper.GetFullFilePath(c.Config.Master.TempFolder, fileName)
	storageLocation := chunk.StorageProfile.GetFullTranscodedFileLocation()

	// Build result file name
	outputFileName := chunk.TranscodingFileID + "." + chunk.TranscodeProfile.OutputFileExtension
	outputFileName = path.Join(storageLocation.Path, chunk.TranscodeProfile.Variation, outputFileName)

	// Upload to bucket
	if err := c.StorageController.Upload(storageLocation.Bucket, filePath, outputFileName, models.ACL_PUBLIC_READ); err != nil {
		return err
	}

	// Get public url
	url := c.StorageController.GetPublicUrl(storageLocation.Bucket, outputFileName)

	transcodedFile := models.TranscodedFile{
		BaseModel:              models.BaseModel{ID: helper.GenerateUUID()},
		OriginalFileID:         chunk.TranscodingFileID,
		OriginalFileTemplateID: chunk.TranscodeProfile.TranscodeTemplateID,
		Url:                    url,
		Variation:              chunk.TranscodeProfile.Variation,
		TranscodedFileMetaData: models.TranscodedFileMetaData{
			BaseModel:        models.BaseModel{ID: helper.GenerateUUID()},
			MediaMetaData:    meta,
			TranscodedFileID: chunk.TranscodingFileID,
		},
	}
	c.DbController.FilesDao.InsertTranscodedFile(transcodedFile)

	//Notify Outside Services
	message := remote.NewTranscodedFile(chunk.TranscodingFileID, chunk.PostID, chunk.TranscodeProfile.Variation, url, meta)
	jsonMessage, _ := json.Marshal(message)
	return c.Bus.SendData(models.FILE_TRANSCODED_EVENT, jsonMessage)
}
