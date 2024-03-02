package helper

import (
	"os"
	"path"
	"strings"
)

func GetFullFilePath(parts ...string) string {
	return path.Join(parts...)
	//return base + string(os.PathSeparator) + objectName
}

func GetTranscodedFileName(fileID, transcodeProfileID, extension string) string {
	return fileID + transcodeProfileID + "." + extension
}

func GetAdaptiveTranscodedFileName(fileID, transcodeProfileID, extension string) string {
	return fileID + transcodeProfileID + "_dash." + extension
}

func GetChunkName(id, extension string) string {
	return id + "." + extension
}

func GetChunksListFilePath(fileID string, transcodeProfileID string, tmpFolder string) string {
	parameters := []string{tmpFolder, string(os.PathSeparator), fileID, transcodeProfileID}
	name := strings.Join(parameters, "")
	return name
}
