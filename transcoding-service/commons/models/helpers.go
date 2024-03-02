package models

import (
	"os"
	"time"
)

func GetNextCallback(index int) *time.Time {
	if index >= len(CALLBACK_RETRIES) {
		return &time.Time{}
	}
	nextCallback := time.Now().Add(CALLBACK_RETRIES[index])
	return &nextCallback
}

func GetConfigLocation() string {
	location := os.Getenv("CONFIG_LOCATION")
	if location == "" {
		return DEFAULT_CONFIG_LOCATION
	}
	return location
}
