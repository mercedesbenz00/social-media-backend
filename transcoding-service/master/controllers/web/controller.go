package web

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"

	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/bus"
	ceph "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/file-storage"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/helper"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models/remote"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/db"
	swagger "github.com/arsmn/fiber-swagger/v2"
	"github.com/gofiber/fiber/v2"
)

type WebController struct {
	Config            models.Config
	DbController      db.Controller
	StorageController ceph.Controller
	Bus               *bus.Rabbit
}

// transcodeBody represents transcode body to transcode by URL
//
// swagger:model
type transcodeBody struct {
	// ID
	// Required: true
	ID string `json:"id"`
	// ID
	// Required: true
	PostID int `json:"postId"`
	// File URL
	// Required: true
	URL string `json:"url"`
	// Callback URL
	// Required: false
	CallbackURL string `json:"callback_url,omitempty"`
}

// swagger:parameters getTranscodeBody
type getTranscodeBody struct {
	// in: body
	Body transcodeBody
}

// transcodeBodyBucket represents transcode body to transcode by Object in bucket
//
// swagger:model
type transcodeBodyBucket struct {
	// ID
	// Required: true
	ID string `json:"id"`
	// ID
	// Required: true
	PostID int `json:"postId"`
	// Bucket name
	// Required: false
	Bucket string `json:"bucket,omitempty"`
	// Object name
	// Required: true
	ObjectName string `json:"objectName"`
	// Callback URL
	// Required: false
	CallbackURL string `json:"callbackUrl,omitempty"`
}

// swagger:parameters getTranscodeBodyBucket
type getTranscodeBodyBucket struct {
	// in: body
	Body transcodeBodyBucket
}

// swagger:parameters TranscodeResponseBody
type TranscodeResponseBody struct {
	// Status
	Status bool `json:"status"`
	// Error
	Error string `json:"error,omitempty"`
}

// swagger:response getTranscodeResponseBody
type getTranscodeResponseBody struct {
	// in: body
	Body TranscodeResponseBody
}

func getDefaultTranscodingFile(ID string, postID int, callbackURL string, configuration *models.Config) models.TranscodingFile {
	transcodingFile := models.TranscodingFile{}
	transcodingFile.ID = ID
	transcodingFile.PostID = postID

	if callbackURL == "" {
		transcodingFile.CallbackURL = configuration.CallbackInfo.CallbackURL
	} else {
		transcodingFile.CallbackURL = callbackURL
	}

	template := models.TranscodeTemplate{}
	template.Encoder = models.VERTICAL_RESOLUTIONS_ENCODER
	template.TranscodeCommand = models.WEB_TRANSCODE_COMMAND
	template.OutputFileExtension = models.WEB_OUTPUT_EXTENSION
	transcodingFile.TranscodeTemplates = append(transcodingFile.TranscodeTemplates, template)

	return transcodingFile
}

func (w *WebController) ValidateAndSend(transcodingFile models.TranscodingFile) error {
	if w.DbController.FilesDao.GetFile(transcodingFile.ID).ID == transcodingFile.ID {
		return fmt.Errorf("file already exists: %s", transcodingFile.ID)
	}
	bytes, err := json.Marshal(transcodingFile)
	if err != nil {
		return err
	}

	if err = w.Bus.SendData(models.TRANSCODE_FILE_REQUEST, bytes); err != nil {
		return err
	}
	return nil
}

func (w *WebController) Init(configuration *models.Config) {

	log.Println("LOG: Starting web server")
	app := fiber.New()

	// GET docs
	app.Get("/swagger.yaml", func(ctx *fiber.Ctx) error {
		content, err := ioutil.ReadFile("swagger.yaml")
		if err != nil {
			return nil
		}
		ctx.Send([]byte(content))
		return nil
	})
	app.Get("/swagger/*", swagger.New(swagger.Config{ // custom
		URL:         "/swagger.yaml",
		DeepLinking: false,
	}))

	// GET /file/:id
	app.Get("/files/:id", func(ctx *fiber.Ctx) error {
		id := ctx.Params("id")

		file := w.DbController.FilesDao.GetFile(id)
		//Not found
		if file.ID != id {
			ctx.SendStatus(404)
			return nil
		}

		doneFiles := w.DbController.FilesDao.GetTranscodedFilesForSourceId(id)
		responseDto := remote.FileProgressDto{
			BaseModel: models.BaseModel{ID: id},
			Status:    file.FileStatus,
			DoneFiles: doneFiles,
		}

		jsonString, _ := json.Marshal(responseDto)
		ctx.Send(jsonString)
		return nil
	})

	// TranscodeVideoFromBucket swagger:route POST /transcode/from_bucket transcoding getTranscodeBodyBucket
	// Transcode object from bucket.
	//
	// Transcode object from bucket.
	//
	//	Responses:
	//	  200: getTranscodeResponseBody
	app.Post("/transcode/from_bucket", func(ctx *fiber.Ctx) error {
		var request getTranscodeBodyBucket
		if err := json.Unmarshal(ctx.Body(), &request.Body); err != nil {
			return err
		}
		body := request.Body

		transcodingFile := getDefaultTranscodingFile(body.ID, body.PostID, body.CallbackURL, configuration)

		if body.Bucket != "" {
			var storageLocations []models.StorageLocation
			for _, storageLocation := range w.Config.DefaultStorageLocations {
				if storageLocation.Type != models.TYPE_LOCATION_SOURCE {
					storageLocation.ID = helper.GenerateUUID()
					storageLocations = append(storageLocations, storageLocation)
				}
			}
			sourceStorageLocation := models.StorageLocation{
				Type:       models.TYPE_LOCATION_SOURCE,
				Bucket:     body.Bucket,
				ObjectName: body.ObjectName,
			}
			sourceStorageLocation.ID = helper.GenerateUUID()
			storageLocations = append(storageLocations, sourceStorageLocation)
			transcodingFile.StorageProfile.StorageLocations = storageLocations
		}

		var transcodeBodyResponse getTranscodeResponseBody
		err := w.ValidateAndSend(transcodingFile)
		if err != nil {
			ctx.Status(http.StatusBadRequest)
			transcodeBodyResponse.Body.Error = err.Error()
		} else {
			transcodeBodyResponse.Body.Status = true
		}

		bb, err := json.Marshal(transcodeBodyResponse.Body)
		if err != nil {
			return err
		}
		ctx.Send(bb)

		return nil
	})

	// TranscodeVideoFromURL swagger:route POST /transcode transcoding getTranscodeBody
	// Transcode object by URL.
	//
	// Transcode object by URL.
	//
	//	Responses:
	//	  200: getTranscodeResponseBody
	app.Post("/transcode", func(ctx *fiber.Ctx) error {
		var request getTranscodeBody
		body := request.Body
		if err := json.Unmarshal(ctx.Body(), &body); err != nil {
			return err
		}

		transcodingFile := getDefaultTranscodingFile(body.ID, body.PostID, body.CallbackURL, configuration)
		transcodingFile.StorageProfile.Url = body.URL

		var transcodeBodyResponse getTranscodeResponseBody
		err := w.ValidateAndSend(transcodingFile)
		if err != nil {
			ctx.Status(http.StatusBadRequest)
			transcodeBodyResponse.Body.Error = err.Error()
		} else {
			transcodeBodyResponse.Body.Status = true
		}

		bb, err := json.Marshal(transcodeBodyResponse.Body)
		if err != nil {
			return err
		}
		ctx.Send(bb)

		return nil
	})

	err := app.Listen(fmt.Sprintf(":%d", models.WEB_DEFAULT_PORT))
	log.Printf("FATAL: fail to start web server %s", err.Error())

}
