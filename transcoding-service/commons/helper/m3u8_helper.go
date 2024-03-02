package helper

type ItemDiscontinuityTag struct{}

func (i ItemDiscontinuityTag) String() string {
	return "#EXT-X-DISCONTINUITY"
}
