package controllers

import (
	"encoding/json"
	"log"
	"os"
	"path"
	"time"

	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models/remote"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/transcode"
	"github.com/streadway/amqp"
)

func (c *MasterController) HandleExternalMessage(i amqp.Delivery) {

	log := log.New(os.Stdout, "transcoder:", log.LstdFlags|log.Lshortfile|log.Lmicroseconds)

	var b = i.Body

	log.Printf(string(i.Body))

	var sourceFile models.TranscodingFile
	err := json.Unmarshal(b, &sourceFile)

	if err != nil {
		c.Errors <- err
		log.Printf("ERROR: Message parsing error %s", err.Error())
		return
	}

	if c.DbController.FilesDao.GetFile(sourceFile.ID).ID == sourceFile.ID {
		log.Printf("ERROR: File already exists %s", sourceFile.ID)
		//File already exists
		return
	}

	if &sourceFile != nil {

		sourceFile.FileStatus = models.FILE_PENDING

		for i, _ := range sourceFile.TranscodeTemplates {
			sourceFile.TranscodeTemplates[i].ID = helper.GenerateUUID()
		}

		if sourceFile.StorageProfile.ID == "" {
			sourceFile.StorageProfile.ID = helper.GenerateUUID()
		}

		if sourceFile.StorageProfile.StorageLocations == nil || len(sourceFile.StorageProfile.StorageLocations) == 0 {
			sourceFile.StorageProfile.StorageLocations = c.Config.DefaultStorageLocations
			for i, _ := range sourceFile.StorageProfile.StorageLocations {
				sourceFile.StorageProfile.StorageLocations[i].ID = helper.GenerateUUID()
			}
		}

		c.DbController.FilesDao.InsertFile(sourceFile)

		var numberOfNodes, err = c.Bus.GetNodesNumber()
		if err != nil {
			log.Printf("error getting nodes: %s", err)
			return
		}

		for numberOfNodes < 1 {
			log.Println("LOG: Waiting for at least one node to be up")
			time.Sleep(10 * time.Second)
			numberOfNodes, err = c.Bus.GetNodesNumber()
			if err != nil {
				log.Printf("error getting nodes: %s", err)
				continue
			}
		}
		transcodeController := transcode.NewTranscodeController(c.Config.Node.TempFolder, c, c.Config.Master, c.DbController)

		err = transcodeController.StartTranscoding(sourceFile, numberOfNodes)
		if err != nil {
			log.Printf("ERROR: An error happened while encoding the file %s", err.Error())
		}
	}
}

//Transcode Handle
func (c *MasterController) GetSourceLink(profile models.StorageProfile) (string, error) {
	location := profile.GetSourceLocation()
	return c.StorageController.SignUrl(location.Bucket, path.Join(location.Path, location.ObjectName))
}

func (c *MasterController) SendChunk(chunk remote.FileChunkDto) {
	jsonString, _ := json.Marshal(chunk)
	c.Bus.SendData(models.TRANSCODE_CHUNK_REQUEST, jsonString)
}
