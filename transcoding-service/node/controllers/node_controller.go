package controllers

import (
	"encoding/json"
	"io/ioutil"
	"log"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"strings"

	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/bus"
	filestorage "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/file-storage"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models/remote"
	"github.com/streadway/amqp"
)

type NodeController struct {
	Bus               bus.Bus
	StorageController *filestorage.Controller
	NodeInfo          models.NodeInfo
}

func (c *NodeController) HandleMessage(d amqp.Delivery) {
	c.transcode(d)
}

func (c *NodeController) StartOperation() error {
	c.createFolders()
	if err := c.Bus.Init(); err != nil {
		log.Printf("FATAL: didn't manage to init bus %s", err.Error())
		return err
	}
	return c.Bus.ReceiveData(models.TRANSCODE_CHUNK_REQUEST, c.HandleMessage)
}

func (c *NodeController) createFolders() error {
	return os.Mkdir(c.NodeInfo.TempFolder, 0600)
}

func (c *NodeController) transcode(d amqp.Delivery) {
	var jsonStr []byte = d.Body
	var chunk remote.FileChunkDto

	err := json.Unmarshal(jsonStr, &chunk)
	if err != nil {
		log.Println("ERROR: Failed to decode the message")
		return
	}

	Chunk := &chunk

	profile := chunk.TranscodeProfile

	if err = os.MkdirAll(c.getTempPath(Chunk), os.ModePerm); err != nil {
		log.Println("ERROR: Failed to create temp folder")
		return
	}

	parameters := profile.ChunkTranscodeParameters
	n := len(parameters)
	parametersArray := make([]string, n)
	for i, parameter := range parameters {
		parametersArray[i] = parameter.Value
	}

	cmd := exec.Command(profile.TranscodeCommand, parametersArray...)

	log.Printf("LOG: transcoding source id/chunk id/params - %d/%s/%s", Chunk.PostID, Chunk.ID, strings.Join(parametersArray, " "))
	output, err := cmd.CombinedOutput()

	if err != nil {
		logAndSendChunkError(chunk, c, err, output)
		return
	}

	err = c.uploadChunk(Chunk)

	if err != nil {
		logAndSendChunkError(chunk, c, err, nil)
		return
	}

	os.RemoveAll(c.getTempPath(&chunk))
	c.updateStatusInParent(chunk, models.CHUNK_DONE)
	err = d.Ack(false)
	if err != nil {
		log.Println("ERROR: Message couldn't be acknowledged")
	}
}

func logAndSendChunkError(chunk remote.FileChunkDto, c *NodeController, err error, output []byte) {
	log.Printf("ERROR: An error happened while transcoding the file - %s:\n%s", err.Error(), string(output))
	chunk.Error = *models.NewChunkError(chunk.ID, "chunk_transcode_error", "")
	c.updateStatusInParent(chunk, models.CHUNK_ERROR)
}

func (c *NodeController) updateStatusInParent(chunk remote.FileChunkDto, status string) {
	chunk.ChunkStatus = status
	byteArray, err := json.Marshal(chunk)
	if err != nil {
		log.Printf("ERROR: didn't manage to transform chunk to byte array - %s", err.Error())
	}
	c.Bus.SendData(models.CHUNK_TRANSCODED_EVENT, byteArray)
}

func (c *NodeController) getTempPath(chunk *remote.FileChunkDto) string {
	return helper.GetFullFilePath(c.NodeInfo.TempFolder, chunk.TranscodingFileID, chunk.ID)
}

func (c *NodeController) uploadChunk(chunk *remote.FileChunkDto) (err error) {
	name := c.getTranscodedChunkName(*chunk)
	chunksLocation := chunk.StorageProfile.GetChunksLocation()
	resultFile := helper.GetFullFilePath(c.getTempPath(chunk), name)
	if chunk.TranscodeProfile.OutputFileExtension == models.EXTENSTION_M3U8 {
		// if we're working with m3u8 we need to handle *.ts files
		// moving them directly to processed folder since we don't need to stitch them later
		// and adjust m3u8 content before uploading
		transcodedFileLocation := chunk.StorageProfile.GetFullTranscodedFileLocation()
		m3u8Replacements := make(map[string]string)

		// move all ts to transcoded bucket
		walkErr := filepath.Walk(c.getTempPath(chunk), func(filePath string, info os.FileInfo, err error) error {
			if info.IsDir() {
				return nil
			}
			nameAndExt := info.Name()
			if !strings.HasSuffix(nameAndExt, ".ts") {
				return nil
			}

			fileKey := path.Join(transcodedFileLocation.Path, chunk.TranscodeProfile.Variation, chunk.TranscodingFileID, nameAndExt)
			if err = c.StorageController.Upload(transcodedFileLocation.Bucket, filePath, fileKey, models.ACL_PUBLIC_READ); err != nil {
				return err
			}
			m3u8Replacements[nameAndExt] = c.StorageController.GetPublicUrl(transcodedFileLocation.Bucket, fileKey)

			return nil
		})
		if walkErr != nil {
			return walkErr
		}
		// replace content in m3u8
		bData, err := ioutil.ReadFile(resultFile)
		if err != nil {
			return err
		}
		sData := string(bData)
		for key, value := range m3u8Replacements {
			sData = strings.Replace(sData, key, value, 1)
		}
		bData = []byte(sData)
		if err = ioutil.WriteFile(resultFile, bData, 0); err != nil {
			return err
		}
	}

	err = c.StorageController.Upload(chunksLocation.Bucket, resultFile, path.Join(chunksLocation.Path, name), "")

	return err
}

func (c *NodeController) getTranscodedChunkName(chunk remote.FileChunkDto) string {
	return chunk.ID + "." + chunk.TranscodeProfile.OutputFileExtension
}
