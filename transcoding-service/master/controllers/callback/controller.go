package callback

import (
	"bytes"
	"encoding/json"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/bus"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/db"
)

type CallbackController struct {
	DbController        db.Controller
	SecurityHeaderValue string
	httpClient          http.Client
	Bus                 *bus.Rabbit
}

func (c *CallbackController) Init() {

	c.httpClient = http.Client{}

	tick := time.NewTicker(models.CALLBACK_PERIOD)
	c.task(time.Now())
	go c.scheduler(tick)

	signals := make(chan os.Signal, 1)
	signal.Notify(signals, syscall.SIGINT, syscall.SIGTERM)
	<-signals
	tick.Stop()
}

func (c *CallbackController) scheduler(tick *time.Ticker) {
	for t := range tick.C {
		c.task(t)
	}
}

func (c *CallbackController) buildCallbackBody(transcodingFile models.TranscodingFile) CallbackBody {
	body := CallbackBody{
		ID:          transcodingFile.ID,
		PostID:      transcodingFile.PostID,
		Duration:    transcodingFile.Duration,
		M3U8URL:     transcodingFile.M3U8URL,
		EncodedURLs: make(map[string]string),
	}

	for _, transcodedFile := range c.DbController.FilesDao.GetTranscodedFilesForSourceId(transcodingFile.ID) {
		body.EncodedURLs[transcodedFile.Variation] = transcodedFile.Url
	}
	return body
}

func (c *CallbackController) task(t time.Time) {
	for _, transcodingFile := range c.DbController.FilesDao.GetCallbackFiles() {
		if transcodingFile.CallbackURL == "" {
			log.Printf("---[FAILED] Missing callback url for %s", transcodingFile.ID)
			c.DbController.FilesDao.FailCallback(transcodingFile.ID)
			continue
		}
		body := c.buildCallbackBody(transcodingFile)

		buf, err := json.Marshal(body)
		if err != nil {
			log.Printf("---[FAILED] Failed to generate callback body for %s", transcodingFile.ID)
			continue
		}
		req, err := http.NewRequest("POST", transcodingFile.CallbackURL, bytes.NewBuffer(buf))
		if err != nil {
			log.Printf("---[WARNING] Creating request for %s: %s", transcodingFile.ID, err)
			continue
		}

		req.Header.Add(models.CALLBACK_SECURITY_HEADER_NAME, c.SecurityHeaderValue)
		req.Header.Add("content-type", "application/json")
		resp, err := c.httpClient.Do(req)
		if err != nil {
			log.Printf("---[WARNING] Calling callback for %s: %s", transcodingFile.ID, err)
			log.Printf("---[WARNING] Rescheduling callback %s for %s", transcodingFile.CallbackURL, transcodingFile.ID)
			if !c.DbController.FilesDao.ScheduleCallback(transcodingFile.ID, transcodingFile.CallbackCounter+1) {
				log.Printf("---[FAILED] Failed to reschedule for %s", transcodingFile.ID)
			}
			continue
		}

		if resp.StatusCode != 200 {
			log.Printf("---[WARNING] Rescheduling callback %s for %s", transcodingFile.CallbackURL, transcodingFile.ID)
			if !c.DbController.FilesDao.ScheduleCallback(transcodingFile.ID, transcodingFile.CallbackCounter+1) {
				log.Printf("---[FAILED] Failed to reschedule for %s", transcodingFile.ID)
			}
			resp.Body.Close()
			continue
		}
		c.DbController.FilesDao.SuccessCallback(transcodingFile.ID)
		log.Printf("---[SUCCESS] Callback %s for %s", transcodingFile.CallbackURL, transcodingFile.ID)
		resp.Body.Close()
	}
}
