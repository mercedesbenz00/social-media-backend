package metadata

type FileValidationError struct {
	S string
}

func (e *FileValidationError) Error() string {
	return e.S
}
