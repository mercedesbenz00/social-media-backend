package models

import "time"

type Status string

const (
	FILE_DOWNLOADED  = "FILE_DOWNLOADED"
	FILE_PENDING     = "FILE_PENDING"
	FILE_ERROR       = "FILE_ERROR"
	FILE_TRANSCODING = "FILE_TRANSCODING"
	FILE_DONE        = "FILE_DONE"
)

const (
	CHUNK_ERROR       = "CHUNK_ERROR"
	CHUNK_TRANSCODING = "CHUNK_TRANSCODING"
	CHUNK_DONE        = "CHUNK_DONE"
)

type QueueInfo struct {
	Name          string
	AutoAck       bool
	PrefetchSize  int
	PrefetchCount int
}

var TRANSCODE_CHUNK_REQUEST = QueueInfo{
	Name:          "TRANSCODE_CHUNK_REQUEST",
	AutoAck:       false,
	PrefetchSize:  8,
	PrefetchCount: 8,
}

var TRANSCODE_FILE_REQUEST = QueueInfo{
	Name:          "TRANSCODE_FILE_REQUEST",
	AutoAck:       true,
	PrefetchSize:  -1,
	PrefetchCount: -1,
}

var TRANSCODED_FILE_EVENT = QueueInfo{
	Name:          "TRANSCODED_FILE_EVENT",
	AutoAck:       true,
	PrefetchSize:  -1,
	PrefetchCount: -1,
}

var CHUNK_TRANSCODED_EVENT = QueueInfo{
	Name:          "CHUNK_TRANSCODED_EVENT",
	AutoAck:       true,
	PrefetchSize:  -1,
	PrefetchCount: -1,
}

var FILE_TRANSCODED_EVENT = QueueInfo{
	Name:          "FILE_TRANSCODED_EVENT",
	AutoAck:       true,
	PrefetchSize:  -1,
	PrefetchCount: -1,
}

var POST_VIDEO_TRANSCODED_EVENT = QueueInfo{
	Name:          "POST_VIDEO_TRANSCODED_EVENT",
	AutoAck:       true,
	PrefetchSize:  -1,
	PrefetchCount: -1,
}

//var CHUNK_ACCEPTED_EVENT = QueueInfo{
//	Name:          "CHUNK_ACCEPTED_EVENT",
//	AutoAck:       true,
//	PrefetchSize:  -1,
//	PrefetchCount: -1,
//}
//
//var CHUNK_ERROR_EVENT = QueueInfo{
//	Name:          "CHUNK_ERROR_EVENT",
//	AutoAck:       true,
//	PrefetchSize:  -1,
//	PrefetchCount: -1,
//}

const (
	FILE_ERROR_STITCHING = "FILE_ERROR_STITCHING"
	// Extensions
	EXTENSTION_M3U8 = "m3u8"
	// Encoders
	DEFAULT_ENCODER              = "x264"
	ADAPTIVE_ENCODER             = "x264_adaptive"
	VERTICAL_RESOLUTIONS_ENCODER = "x264_res"
	// Minio
	MINIO_DEFAULT_EXPIRE_PRESIGNED = time.Hour * 24 * 7 //7 days
	// web
	WEB_TRANSCODE_COMMAND = "ffmpeg"
	WEB_OUTPUT_EXTENSION  = EXTENSTION_M3U8
	WEB_DEFAULT_PORT      = 8033
	// callbacks
	CALLBACK_DEFAULT_URL          = "http://localhost"
	CALLBACK_SECURITY_HEADER_NAME = "X-TRANSCODER-REQUESTHASH"
	CALLBACK_PERIOD               = time.Minute
	// minio acl
	ACL_PUBLIC_READ = "public-read"
	// ffmpeg
	FFMPEG_WHITELIST_PROTOCOLS = "file,http,tcp,https,tls"
	// config location
	DEFAULT_CONFIG_LOCATION = "config.json"
	// chunk
	MINIMUM_CHUNK_SIZE = 6.0 // seconds
	// Rabbit config
	RABBIT_MASTER_PREFETCH_COUNT = 0 // doesn't matter since we have one goroutine per queue anyway
	RABBIT_NODE_PREFETCH_COUNT   = 1 // round-robin
)

var CALLBACK_RETRIES = []time.Duration{
	time.Minute * 0,
	time.Minute * 2,
	time.Minute * 5,
	time.Minute * 10,
}

//  Mapping of vertical resolution to variation name (will be used as sub folder)
var VERTICAL_RESOLUTIONS = map[int]string{
	2160: "2160p",
	1080: "1080p",
	720:  "720p",
	480:  "480p",
	360:  "360p",
}

// Bandwith
var VERTICAL_RES_TO_BANDWIDTH = map[string]int{
	"2160p": 10285391,
	"1080p": 6214307,
	"720p":  1558322,
	"480p":  1144430,
	"360p":  831270,
}
