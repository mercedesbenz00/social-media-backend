package models

type TimeSplitInformation struct {
	BaseModel
	StartTime   float64 `json:"start_time" gorm:"start_time"`
	EndTime     float64 `json:"end_time"`
	FileChunkID string
}

func NewTimeSplitInformation(id string, startTime float64, endTime float64) *TimeSplitInformation {
	return &TimeSplitInformation{BaseModel: BaseModel{ID: id}, StartTime: startTime, EndTime: endTime}
}
