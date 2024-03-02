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

type H264AdaptiveStitcher struct {
	handle StitchHandle
}

func (h *H264AdaptiveStitcher) arePartsDone(chunk remote.FileChunkDto) bool {
	return h.handle.GetDbController().ChunksDao.GetUndoneChunksCount(chunk.TranscodingFileID) == 0
}

func (h *H264AdaptiveStitcher) StitchIfPartsDone(chunk remote.FileChunkDto) error {
	if h.arePartsDone(chunk) {
		return h.Stitch(chunk)
	}
	return nil
}

func (h *H264AdaptiveStitcher) Stitch(chunk remote.FileChunkDto) error {
	tmp := h.handle.GetTempFolderLocation()
	fileID := chunk.TranscodingFileID
	db := h.handle.GetDbController()

	var err error

	allChunks := db.ChunksDao.GetDoneChunksForFileForSpecificEncoder(fileID, chunk.TranscodeProfile.Encoder)

	for _, variationChunks := range allChunks {

		urls := ""

		f, _ := os.Create(GetChunksListFilePath(fileID, variationChunks[0].TranscodeProfile.TranscodeTemplateID, tmp) + "")

		for _, value := range variationChunks {
			strgProfile := db.StorageProfileDao.GetProfile(value.StorageProfileID)
			trnsProfile := db.TranscodeProfileDao.GetProfileForChunk(chunk.TranscodeProfile.ID)
			chunksLocation := strgProfile.GetChunksLocation()
			url, err := h.handle.SignUrl(chunksLocation.Bucket, path.Join(chunksLocation.Path, GetChunkName(value.ID, trnsProfile.OutputFileExtension)))
			if err != nil {
				return err
			}
			urls = urls + "file  " + url + "\n"
		}
		_, err = f.WriteString(urls)
		if err != nil {
			return err
		}

		err = f.Close()
		if err != nil {
			return err
		}
	}

	for _, variationChunks := range allChunks {

		fileName := helper.GetTranscodedFileName(fileID, variationChunks[0].TranscodeProfile.TranscodeTemplateID, chunk.TranscodeProfile.OutputFileExtension)
		cmd := exec.Command("ffmpeg", "-f", "concat", "-safe", "0", "-protocol_whitelist", models.FFMPEG_WHITELIST_PROTOCOLS, "-i", GetChunksListFilePath(fileID, variationChunks[0].TranscodeProfile.TranscodeTemplateID, tmp), "-c", "copy", helper.GetFullFilePath(tmp, fileName))
		output, err := cmd.CombinedOutput()
		if err != nil {
			log.Println(err.Error())
			log.Println(string(output))
			return err
		}
		log.Println(string(output))
	}

	h.generateAdaptive(allChunks)

	return nil
}

func (h *H264AdaptiveStitcher) generateAdaptive(chunksArray [][]models.FileChunk) error {
	tmp := h.handle.GetTempFolderLocation()

	chunk := chunksArray[0][0]
	fileId := chunk.TranscodingFileID

	mpdOutput := fileId
	filesLines := []string{}

	for _, chunks := range chunksArray {
		inputName := helper.GetTranscodedFileName(fileId, chunks[0].TranscodeProfile.TranscodeTemplateID, chunk.TranscodeProfile.OutputFileExtension)
		outputFile := helper.GetAdaptiveTranscodedFileName(fileId, chunks[0].TranscodeProfile.TranscodeTemplateID, chunk.TranscodeProfile.OutputFileExtension)

		line := "in=" + GetFullFilePath(tmp, inputName) + ",stream=video,out=" + GetFullFilePath(tmp, outputFile)
		filesLines = append(filesLines, line)
	}

	filesLines = append(filesLines, "--mpd_output")
	filesLines = append(filesLines, helper.GetFullFilePath(tmp, mpdOutput+".mpd"))

	cmd := exec.Command("packager", filesLines...)

	output, err := cmd.CombinedOutput()
	if err != nil {
		log.Println(err.Error())
		log.Println(string(output))
		return err
	}

	return nil
}
