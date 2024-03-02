package controllers

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/bus"
	ceph "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/file-storage"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/db"
	"os"
)

type MasterController struct {
	Config            models.Config
	Bus               bus.Bus
	DbController      db.Controller
	StorageController ceph.Controller
	ShouldClean       bool

	Errors chan error
}

func (c *MasterController) StartOperation() {

	if c.ShouldClean {
		c.DeleteTempData()
	}

	b := &c.Bus

	err := c.createFolders()
	if err != nil {
		c.Errors <- err
		return
	}

	forever := make(chan bool)
	//each handler exists in a separate file in the same package
	go func() {
		c.Errors <- (*b).ReceiveData(models.TRANSCODE_FILE_REQUEST, c.HandleExternalMessage)
	}()

	go func() {
		c.Errors <- (*b).ReceiveData(models.CHUNK_TRANSCODED_EVENT, c.HandleNodeMessage)
	}()
	<-forever

}

func (c *MasterController) createFolders() error {
	err := os.MkdirAll(c.Config.Master.TempFolder, os.ModePerm)

	//For some reason in the above line, giving permission didn't work
	err = os.Chmod(c.Config.Master.TempFolder, os.ModePerm)
	return err
}

func (c *MasterController) DeleteTempData() error {
	return os.RemoveAll(c.Config.Master.TempFolder)
}

func (c *MasterController) GetDbController() *db.Controller {
	return &c.DbController
}
