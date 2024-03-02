package callback

type CallbackBody struct {
	ID          string            `json:"video_id"`
	PostID      int               `json:"post_id"`
	Duration    float64           `json:"duration"`
	M3U8URL     string            `json:"m3u8"`
	EncodedURLs map[string]string `json:"encoded_urls"`
}
