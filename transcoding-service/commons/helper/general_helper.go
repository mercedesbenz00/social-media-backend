package helper

import (
	"crypto/rand"
	"fmt"
	"log"
)

func GenerateUUID() string {
	b := make([]byte, 16)
	_, err := rand.Read(b)
	if err != nil {
		log.Fatal("FATAL: Didn't manage to generate UUID")
	}
	uuid := fmt.Sprintf("%x%x%x%x%x",
		b[0:4], b[4:6], b[6:8], b[8:10], b[10:])
	return uuid
}
