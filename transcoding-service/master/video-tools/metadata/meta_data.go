package metadata

import (
	"context"
	"gopkg.in/vansante/go-ffprobe.v2"
	"log"
	"strconv"
	"time"
)

type MediaMetaData struct {
	Video      int     `json:"video_tracks"`
	Audio      int     `json:"audio_tracks"`
	VideoCodec string  `json:"video_codec"`
	AudioCodec string  `json:"audio_codec"`
	Channels   int     `json:"channels"`
	Time       float64 `json:"time"`
	Width      int     `json:"width"`
	Height     int     `json:"height"`
	Rate       string  `json:"rate"`
	Pixfmt     string  `json:"pixel_fmt"`
	Data       int     `json:"data"`
}

type CommandData struct {
	Video  string
	Audio  string
	Status int
}

const (
	FFPROBE_WHITELIST_PROTOCOLS = "file,http,tcp,https,tls"
)

func NewMediaMetaData(filePath string) (MediaMetaData, error) {
	ctx, cancelFn := context.WithTimeout(context.Background(), 1000*time.Second)
	defer cancelFn()

	var m MediaMetaData
	data, err := ffprobe.ProbeURL(ctx, filePath, "-protocol_whitelist", FFPROBE_WHITELIST_PROTOCOLS)
	if err != nil {
		log.Printf("ERROR: Failed to get metadata for %s - %s", filePath, err.Error())
		m.Data = 1
		return m, err
	}
	m.Time = data.Format.DurationSeconds
	for i := 0; i < len(data.Streams); i++ {
		if data.Streams[i].CodecType == "video" {
			m.Video = 1
			video_data := data.FirstVideoStream()
			m.VideoCodec = video_data.CodecName
			m.Width = video_data.Width
			m.Height = video_data.Height
			m.Pixfmt = video_data.PixFmt
		}

		if data.Streams[i].CodecType == "audio" {
			m.Audio = 1
			audio_data := data.FirstAudioStream()
			m.AudioCodec = audio_data.CodecName
			m.Rate = audio_data.SampleRate
			m.Channels = audio_data.Channels
		}
	}
	return m, nil
}

func (t *MediaMetaData) CheckFile() error {
	var data CommandData

	var cmd string
	var cmd1 string
	data.Status = 0

	if t.Audio < 1 {
		data.Status = -2
		return &FileValidationError{"NO_AUDIO"}
	}

	if t.Video < 1 {
		data.Status = -2
		return &FileValidationError{"NO_VIDEO"}
	}

	if t.Channels < 1 {
		data.Status = -2
		return &FileValidationError{"NO_CHANNELS"}
	}

	if t.Pixfmt == "yuv444p" {
		cmd = cmd + " -pix_log yuv420p"
	}

	strfloat, _ := strconv.ParseFloat(t.Rate, 64)
	if strfloat > 48000 {
		cmd1 = cmd1 + " -ar 48000"
	}
	data.Video = cmd
	data.Audio = cmd1

	return nil
}

func (t *MediaMetaData) CheckResolution(height int, width int) error {

	if width > t.Width {
		return &FileValidationError{"WRONG_WIDTH"}
	}

	if height > t.Height {
		return &FileValidationError{"WRONG_HEIGHT"}
	}
	return nil
}
