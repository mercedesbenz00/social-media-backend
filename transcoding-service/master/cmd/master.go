package main

import (
	"log"
	"os"

	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/bus"
	jsonconfig "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/config"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/controllers"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/controllers/callback"
	web2 "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/controllers/web"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/master/db"

	ceph "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/file-storage"
)

const (
	//List of available app arguments
	Clean = "--clean"
	Debug = "--debug"
)

var (
	arguementsMap = map[string]bool{
		Clean: false, Debug: false,
	}
)

func main() {
	errors := make(chan error, 1)

	configuration, err := jsonconfig.BuildConfig(models.GetConfigLocation())

	if err != nil {
		log.Println("FATAL: Can't read config file")
		return
	}

	bus := bus.NewRabbit(configuration.EventBus, models.RABBIT_MASTER_PREFETCH_COUNT)

	//init storage
	storageController := ceph.Controller{}
	if err := storageController.Init(configuration.Master.StorageSecrets); err != nil {
		os.Exit(1)
	}

	//init bus
	if err := bus.Init(); err != nil {
		log.Printf("FATAL: didn't manage to init bus %s", err.Error())
		os.Exit(1)
	}

	//insert all command line parameters to the args map
	for _, arg := range os.Args {
		arguementsMap[arg] = true
	}

	//init db
	dbController := db.Controller{ShouldClean: arguementsMap[Clean], DbConfig: configuration.Master.DbConfig}
	if err := dbController.Init(); err != nil {
		log.Printf("FATAL: didn't manage to init DB %s", err.Error())
		os.Exit(1)
	}

	web := web2.WebController{
		Config:            *configuration,
		DbController:      dbController,
		StorageController: storageController,
		Bus:               bus,
	}

	go web.Init(configuration)

	masterController := controllers.MasterController{
		Config:            *configuration,
		Bus:               bus,
		DbController:      dbController,
		StorageController: storageController,
		Errors:            errors,
		ShouldClean:       arguementsMap[Clean],
	}

	go masterController.StartOperation()

	callbackController := callback.CallbackController{
		DbController:        dbController,
		SecurityHeaderValue: configuration.CallbackInfo.SecurityHeader,
	}

	go callbackController.Init()

	select {
	case errSink := <-errors:
		log.Printf("ERROR: %s", errSink.Error())
	}
}
