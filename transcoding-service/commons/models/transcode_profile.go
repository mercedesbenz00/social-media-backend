package models

type BaseTranscodeParametersContainer struct {
	Encoder             string `json:"encoder"`
	OutputFileExtension string `json:"output_file_extension"`
	TranscodeCommand    string `json:"transcode_command"`
	Variation           string `json:"variation" gorm:"variation"`
	TranscodingFileID   string
}

type BaseTranscodeParameter struct {
	Name  string `json:"name"`
	Value string `json:"Value"`
}

type TranscodeTemplate struct {
	BaseModel
	BaseTranscodeParametersContainer
	FileTranscodeParameters []TranscodeTemplateParameter `json:"transcode_parameters" gorm:"foreignkey:file_transcode_template_id"`
}

func (f *TranscodeTemplate) SetParameter(id, name, value string) {
	p := TranscodeTemplateParameter{
		BaseModel: BaseModel{ID: id},
		BaseTranscodeParameter: BaseTranscodeParameter{
			Name:  name,
			Value: value,
		},
		TranscodeTemplateID: "",
	}
	f.FileTranscodeParameters = append(f.FileTranscodeParameters, p)
}

type TranscodeTemplateParameter struct {
	BaseModel
	BaseTranscodeParameter
	TranscodeTemplateID string
}

type ChunkTranscodeProfile struct {
	BaseModel
	BaseTranscodeParametersContainer
	ChunkTranscodeParameters []ChunkTranscodeParameter `json:"transcode_parameters" gorm:"foreignkey:chunk_transcode_profile_id"`
	TranscodeTemplateID      string                    `json:"transcode_template_id"`
	FileChunkID              string
}

type ChunkTranscodeParameter struct {
	BaseModel
	BaseTranscodeParameter
	ChunkTranscodeProfileID string
}

func NewChunkTranscodeProfile(id string, baseTranscodeProfile BaseTranscodeParametersContainer, TranscodeTemplateID string) *ChunkTranscodeProfile {
	return &ChunkTranscodeProfile{BaseModel: BaseModel{ID: id}, BaseTranscodeParametersContainer: baseTranscodeProfile, TranscodeTemplateID: TranscodeTemplateID}
}
