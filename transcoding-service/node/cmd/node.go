package main

import (
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/bus"
	jsonconfig "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/config"
	ceph "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/file-storage"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/node/controllers"
	"log"
	"os"
)

func main() {
	configuration, err := jsonconfig.BuildConfig(models.GetConfigLocation())

	if err != nil {
		return
	}

	//init bus
	bus := bus.NewRabbit(configuration.EventBus, models.RABBIT_NODE_PREFETCH_COUNT)

	//init storage
	storageController := ceph.Controller{}
	location := configuration.Node.StorageSecrets
	if err := storageController.Init(location); err != nil {
		os.Exit(1)
	}

	nodeController := controllers.NodeController{Bus: bus, NodeInfo: configuration.Node, StorageController: &storageController}

	if err := nodeController.StartOperation(); err != nil {
		os.Exit(1)
	}
	log.Println("LOG: Waiting for master to send data")
}
