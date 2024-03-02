package models

const (
	TYPE_LOCATION_SOURCE           = "TYPE_LOCATION_SOURCE"
	TYPE_LOCATION_TRANSCODED_CHUNK = "TYPE_LOCATION_TRANSCODED_CHUNK"
	TYPE_LOCATION_TRANSCODED_FULL  = "TYPE_LOCATION_TRANSCODED_FULL"
	TYPE_LOCATION_M3U8             = "TYPE_LOCATION_M3U8"
)

type StorageProfile struct {
	BaseModel
	Url               string            `json:"url"`
	StorageLocations  []StorageLocation `json:"storage_locations" gorm:"foreignkey:storage_profile_id"`
	TranscodingFileID string
}

type StorageLocation struct {
	BaseModel
	ObjectName string `json:"object_name"`
	Bucket     string `json:"bucket"`
	Path       string `json:"path"`
	StorageSecrets
	Type             string `json:"type"`
	StorageProfileID string `json:"storage_profile_id"`
}

type StorageSecrets struct {
	PublicEndPoint string `json:"public_end_point"`
	EndPoint       string `json:"end_point"`
	AccessKey      string `json:"access_key"`
	SecretKey      string `json:"secret_key"`
	Secure         bool   `json:"secure,omitempty"`
}

func (p StorageProfile) GetChunksLocation() *StorageLocation {
	return p.getLocationOf(TYPE_LOCATION_TRANSCODED_CHUNK)
}

func (p StorageProfile) GetFullTranscodedFileLocation() *StorageLocation {
	return p.getLocationOf(TYPE_LOCATION_TRANSCODED_FULL)
}

func (p StorageProfile) GetSourceLocation() *StorageLocation {
	return p.getLocationOf(TYPE_LOCATION_SOURCE)
}

func (p StorageProfile) GetM3U8Location() *StorageLocation {
	return p.getLocationOf(TYPE_LOCATION_M3U8)
}

func (p StorageProfile) getLocationOf(name string) *StorageLocation {
	for _, location := range p.StorageLocations {
		if location.Type == name {
			return &location
		}
	}
	return nil
}
