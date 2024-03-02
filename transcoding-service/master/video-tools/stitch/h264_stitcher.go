package stitch

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"
	. "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models/remote"
	"log"
	"os"
	"os/exec"
	"path"
)

type H264Stitcher struct {
	handle StitchHandle
}

func (h *H264Stitcher) arePartsDone(chunk remote.FileChunkDto) bool {
	db := h.handle.GetDbController()
	return db.ChunksDao.GetUndoneChunksCountForProfile(chunk.TranscodingFileID, chunk.TranscodeProfile.TranscodeTemplateID) == 0
}

func (h *H264Stitcher) StitchIfPartsDone(chunk remote.FileChunkDto) error {
	db := h.handle.GetDbController()

	if h.arePartsDone(chunk) {
		err := h.Stitch(chunk)
		if err != nil {
			log.Println(err.Error())
			errorModel := *models.NewFileError(chunk.TranscodingFileID, chunk.TranscodeProfile.TranscodeTemplateID, models.FILE_ERROR_STITCHING, "")
			db.ErrorDao.InsertFileError(errorModel)

		}
		return h.handle.UploadAndNotifyServices(chunk)
	}
	return nil
}

func (h *H264Stitcher) Stitch(chunk remote.FileChunkDto) error {
	tempFolder := h.handle.GetTempFolderLocation()
	fileID := chunk.TranscodingFileID
	var err error

	log.Printf("LOG: Stitching files for file_id/profile - %s/%s", fileID, chunk.TranscodeProfile.TranscodeTemplateID)

	filesNames := ""
	chunks := h.handle.GetDbController().ChunksDao.GetDoneChunksForFile(fileID, chunk.TranscodeProfile.TranscodeTemplateID)

	f, err := os.Create(GetChunksListFilePath(fileID, chunk.TranscodeProfile.TranscodeTemplateID, tempFolder))
	if err != nil {
		return err
	}

	for _, value := range chunks {
		strgProfile := h.handle.GetDbController().StorageProfileDao.GetProfile(value.StorageProfileID)
		trnsProfile := h.handle.GetDbController().TranscodeProfileDao.GetProfileForChunk(chunk.TranscodeProfile.ID)
		chunksLocation := strgProfile.GetChunksLocation()
		url, errSignUrl := h.handle.SignUrl(chunksLocation.Bucket, path.Join(chunksLocation.Path, GetChunkName(value.ID, trnsProfile.OutputFileExtension)))

		if errSignUrl != nil {
			return errSignUrl
		}

		filesNames = filesNames + "file  " + url + "\n"
	}

	_, err = f.WriteString(filesNames)
	if err != nil {
		return err
	}

	err = f.Close()
	if err != nil {
		return err
	}

	fileName := helper.GetTranscodedFileName(fileID, chunk.TranscodeProfile.TranscodeTemplateID, chunk.TranscodeProfile.OutputFileExtension)
	cmd := exec.Command("ffmpeg", "-f", "concat", "-safe", "0", "-protocol_whitelist", models.FFMPEG_WHITELIST_PROTOCOLS, "-i", GetChunksListFilePath(fileID, chunk.TranscodeProfile.TranscodeTemplateID, tempFolder), "-c", "copy", helper.GetFullFilePath(tempFolder, fileName))

	output, err := cmd.CombinedOutput()
	if err != nil {
		return err
	}

	log.Println(string(output))

	return nil
}
