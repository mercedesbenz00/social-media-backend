package stitch

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"
	. "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models/remote"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"path"
)

type M3U8Stitcher struct {
	handle StitchHandle
}

func (h *M3U8Stitcher) arePartsDone(chunk remote.FileChunkDto) bool {
	db := h.handle.GetDbController()
	return db.ChunksDao.GetUndoneChunksCountForProfile(chunk.TranscodingFileID, chunk.TranscodeProfile.TranscodeTemplateID) == 0
}

func (h *M3U8Stitcher) StitchIfPartsDone(chunk remote.FileChunkDto) error {
	db := h.handle.GetDbController()

	if h.arePartsDone(chunk) {
		err := h.Stitch(chunk)
		if err != nil {
			log.Printf("ERROR: didn't manage to stich source id/chunk id - %s/%s", chunk.TranscodingFileID, chunk.ID)
			errorModel := *models.NewFileError(chunk.TranscodingFileID, chunk.TranscodeProfile.TranscodeTemplateID, models.FILE_ERROR_STITCHING, "")
			db.ErrorDao.InsertFileError(errorModel)

		}
		return h.handle.UploadAndNotifyServices(chunk)
	}
	return nil
}

func (h *M3U8Stitcher) Stitch(chunk remote.FileChunkDto) error {

	fileID := chunk.TranscodingFileID

	log.Printf("LOG: Stitching files for file_id/profile - %s/%s", fileID, chunk.TranscodeProfile.TranscodeTemplateID)

	chunks := h.handle.GetDbController().ChunksDao.GetDoneChunksForFile(fileID, chunk.TranscodeProfile.TranscodeTemplateID)
	tempFolder := h.handle.GetTempFolderLocation()
	fileName := helper.GetTranscodedFileName(fileID, chunk.TranscodeProfile.TranscodeTemplateID, chunk.TranscodeProfile.OutputFileExtension)

	//playList := m3u8.NewPlaylist()

	for _, value := range chunks {
		strgProfile := h.handle.GetDbController().StorageProfileDao.GetProfile(value.StorageProfileID)
		trnsProfile := h.handle.GetDbController().TranscodeProfileDao.GetProfileForChunk(chunk.TranscodeProfile.ID)
		chunksLocation := strgProfile.GetChunksLocation()
		url, err := h.handle.SignUrl(chunksLocation.Bucket, path.Join(chunksLocation.Path, GetChunkName(value.ID, trnsProfile.OutputFileExtension)))

		if err != nil {
			return err
		}

		resp, err := http.Get(url)
		if err != nil {
			return err
		}

		body, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			_ = resp.Body.Close()
			return err
		}
		_ = resp.Body.Close()

		// Currently there's only one chunk always
		return ioutil.WriteFile(helper.GetFullFilePath(tempFolder, fileName), body, os.ModePerm)

		// TODO: we're removing splitting into chunks due to sound glitch (after stitching m3u8 from multiple nodes)
		//chunkPlayList, err := m3u8.Read(resp.Body)
		//if err != nil {
		//	resp.Body.Close()
		//	return err
		//}
		//resp.Body.Close()
		//for _, item := range chunkPlayList.Items {
		//	playList.AppendItem(helper.ItemDiscontinuityTag{})
		//	playList.AppendItem(item)
		//}
	}

	//return ioutil.WriteFile(helper.GetFullFilePath(tempFolder, fileName), []byte(playList.String()), os.ModePerm)
	return nil
}
