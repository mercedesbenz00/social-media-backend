package controllers

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"github.com/quangngotan95/go-m3u8/m3u8"
)

func (c *MasterController) GenerateM3U8content(transcodingFileID string) string {
	playlist := m3u8.NewPlaylist()
	for _, transcodedFile := range c.DbController.FilesDao.GetTranscodedFilesForSourceId(transcodingFileID) {
		item := &m3u8.PlaylistItem{
			Width:      &transcodedFile.TranscodedFileMetaData.Width,
			Height:     &transcodedFile.TranscodedFileMetaData.Height,
			AudioCodec: &transcodedFile.TranscodedFileMetaData.AudioCodec,
			Bandwidth:  models.VERTICAL_RES_TO_BANDWIDTH[transcodedFile.Variation],
			URI:        transcodedFile.Url,
			Resolution: &m3u8.Resolution{
				Width:  transcodedFile.TranscodedFileMetaData.Width,
				Height: transcodedFile.TranscodedFileMetaData.Height,
			},
		}
		playlist.AppendItem(item)
	}
	return playlist.String()
}
