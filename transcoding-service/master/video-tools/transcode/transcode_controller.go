package transcode

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models/remote"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/db"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/metadata"
	generator2 "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/video-tools/transcode/generator"
)

type Controller struct {
	transcodingHandler TranscodingHandle
	masterInfo         models.NodeInfo
	generator          map[string]generator2.ParametersGenerator
	nodeTempFolder     string
	DbController       db.Controller
}

func NewTranscodeController(nodeTempFolder string, transcodingHandler TranscodingHandle, masterInfo models.NodeInfo, dbController db.Controller) *Controller {
	t := &Controller{
		nodeTempFolder:     nodeTempFolder,
		transcodingHandler: transcodingHandler,
		masterInfo:         masterInfo,
		DbController:       dbController,
	}

	t.generator = map[string]generator2.ParametersGenerator{}
	return t
}

func (c *Controller) StartTranscoding(sourceFile models.TranscodingFile, nodesCount int) error {

	if sourceFile.StorageProfile.Url == "" {
		var err error
		sourceFile.StorageProfile.Url, err = c.transcodingHandler.GetSourceLink(sourceFile.StorageProfile)
		if err != nil {
			return err
		}
	}

	metaData, _ := metadata.NewMediaMetaData(sourceFile.StorageProfile.Url)
	c.DbController.FilesDao.UpdateDuration(sourceFile.ID, metaData.Time)

	//create a parameter generator object if it is not created yet
	for i, profile := range sourceFile.TranscodeTemplates {
		if _, ok := c.generator[profile.Encoder]; !ok {
			generator := generator2.NewTranscoderGenerator(c.nodeTempFolder, &sourceFile.TranscodeTemplates[i])
			c.generator[profile.Encoder] = generator
		}
	}

	c.generateTemplatesVariations(&sourceFile, metaData)
	chunks := c.generateChunksWithTimeData(nodesCount, sourceFile, metaData)
	c.fillChunksWithParametersAndDistribute(chunks, &sourceFile)
	return nil
}

func (c *Controller) fillChunksWithParametersAndDistribute(chunks []remote.FileChunkDto, f *models.TranscodingFile) {
	db := c.transcodingHandler.GetDbController()

	db.FilesDao.UpdateFileStatus(f.ID, models.FILE_TRANSCODING)
	for j, profile := range f.TranscodeTemplates {
		for i, _ := range chunks {
			//change transcoder profile
			c.generator[profile.Encoder].UpdateTranscodeProfile(&f.TranscodeTemplates[j])

			setupChunkAndFillProfiles(&chunks[i], profile, f, c)

			//insert the new generated profile for this chunk
			db.TranscodeProfileDao.InsertProfileForChunk(chunks[i].TranscodeProfile)

			//store and send chunk to children
			db.ChunksDao.InsertChunk(chunks[i].ToLocalChunk())
			c.transcodingHandler.SendChunk(chunks[i])
		}
	}
}

func setupChunkAndFillProfiles(ch *remote.FileChunkDto, profile models.TranscodeTemplate, f *models.TranscodingFile, c *Controller) {
	ch.BaseModel = models.BaseModel{ID: helper.GenerateUUID()}
	chunkProfile := models.NewChunkTranscodeProfile(helper.GenerateUUID(), profile.BaseTranscodeParametersContainer, profile.ID)
	ch.SetProfiles(chunkProfile, &f.StorageProfile)
	ch.TranscodeProfile.ChunkTranscodeParameters = c.generator[profile.Encoder].FillProfileParameters(*ch)
}

func (c *Controller) generateChunksWithTimeData(numberOfNodes int, f models.TranscodingFile, data metadata.MediaMetaData) []remote.FileChunkDto {

	var chunks []remote.FileChunkDto
	breakPoints := []float64{0, data.Time}

	// TODO: we're removing splitting into chunks due to sound glitch (after stitching m3u8 from multiple nodes)
	//numOfChunks := int(data.Time / models.MINIMUM_CHUNK_SIZE)
	//splitSize := models.MINIMUM_CHUNK_SIZE
	//if numOfChunks > numberOfNodes * 2 {
	//	splitSize = float64(int(numOfChunks / numberOfNodes) * models.MINIMUM_CHUNK_SIZE)
	//}
	//
	//for i := 1; true; i++ {
	//	value := float64(i) * splitSize
	//	if value >= data.Time {
	//		breakPoints = append(breakPoints, data.Time)
	//		break
	//	}
	//	breakPoints = append(breakPoints, value)
	//}

	chunks = make([]remote.FileChunkDto, len(breakPoints)-1)
	for i, _ := range chunks {
		timeInformation := models.NewTimeSplitInformation(helper.GenerateUUID(), breakPoints[i], breakPoints[i+1])
		chunks[i] = remote.FileChunkDto{
			ChunkStatus:          models.CHUNK_TRANSCODING,
			TimeSplitInformation: *timeInformation,
			//TODO change this to the actual master name
			MasterNode:        "Master",
			TranscodingFileID: f.ID,
			PostID:            f.PostID,
		}
	}

	return chunks
}

func (c *Controller) generateTemplatesVariations(f *models.TranscodingFile, m metadata.MediaMetaData) {
	for _, transcoder := range c.generator {
		transcoder.ApplyTranscodingPreprocessors(f, m)
	}
	db := c.transcodingHandler.GetDbController()

	db.TranscodeTemplateDao.InsertTranscodeTemplate(f.TranscodeTemplates)
	db.TranscodeTemplateDao.DeleteTranscodeTemplate(f.TranscodeTemplates[0])

	f.TranscodeTemplates = f.TranscodeTemplates[1:]
}

type TranscodingHandle interface {
	GetSourceLink(profile models.StorageProfile) (string, error)
	GetDbController() *db.Controller
	SendChunk(chunk remote.FileChunkDto)
}
