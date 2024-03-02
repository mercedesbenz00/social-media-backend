package config

import (
	"encoding/json"
	"io/ioutil"
	"os"

	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
)

// return Settings from conf.json file
func BuildConfig(configFileName string) (*models.Config, error) {
	var t models.Config

	// Open our jsonFile
	jsonfile, err := os.Open(configFileName)

	// if we os.Open returns an error then handle it
	if err != nil {
		return nil, err
	}

	// defer the closing of our jsonFile so that we can parse it later on
	defer jsonfile.Close()

	value, err := ioutil.ReadAll(jsonfile)
	if err != nil {
		return nil, nil
	}

	err = json.Unmarshal(value, &t)
	if err != nil {
		return nil, err
	}

	return &t, nil
}
