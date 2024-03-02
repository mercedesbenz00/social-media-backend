package db

import (
	"log"

	. "bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"github.com/jinzhu/gorm"
	_ "github.com/jinzhu/gorm/dialects/postgres"
)

type Controller struct {
	Db                   *gorm.DB
	FilesDao             FilesDao
	ChunksDao            ChunksDao
	StorageProfileDao    StorageProfileDao
	TranscodeProfileDao  TranscodeProfileDao
	TranscodeTemplateDao TranscodeTemplateDao
	ErrorDao             ErrorDao
	DbConfig             DbConfig
	ShouldClean          bool
}

// open the database
func (controller *Controller) Init() error {

	db := controller.Db
	var err error
	db, err = gorm.Open(controller.DbConfig.Dialect, controller.DbConfig.ConnectionString)

	if err != nil {
		log.Printf("FATAL: Couldn't start up the database - %s", err.Error())
		return err
	}

	controller.deleteTablesIfDebugEnabled(db)
	controller.createTables(db)
	controller.createDaos(db)
	log.Println("LOG: The database is up")
	return nil
}

func (controller *Controller) createDaos(db *gorm.DB) {
	controller.FilesDao = FilesDao{Db: db}
	controller.ChunksDao = ChunksDao{Db: db}
	controller.StorageProfileDao = StorageProfileDao{Db: db}
	controller.TranscodeProfileDao = TranscodeProfileDao{Db: db}
	controller.TranscodeTemplateDao = TranscodeTemplateDao{Db: db}
	controller.ErrorDao = ErrorDao{Db: db}
}

func (controller *Controller) createTables(db *gorm.DB) {
	db.AutoMigrate(&TranscodingFile{})
	db.AutoMigrate(&TranscodeTemplate{})
	db.AutoMigrate(&StorageProfile{})
	db.AutoMigrate(&FileChunk{})
	db.AutoMigrate(&ChunkTranscodeProfile{})
	db.AutoMigrate(&TimeSplitInformation{})
	db.AutoMigrate(&TranscodeTemplateParameter{})
	db.AutoMigrate(&StorageLocation{})
	db.AutoMigrate(&ChunkTranscodeParameter{})
	db.AutoMigrate(&ChunkError{})
	db.AutoMigrate(&FileError{})
	db.AutoMigrate(&TranscodedFile{})
	db.AutoMigrate(&TranscodedFileMetaData{})

}

func (controller *Controller) deleteTablesIfDebugEnabled(db *gorm.DB) {
	if controller.ShouldClean {
		db.DropTableIfExists(&TranscodingFile{})
		db.DropTableIfExists(&TranscodeTemplate{})
		db.DropTableIfExists(&StorageProfile{})
		db.DropTableIfExists(&FileChunk{})
		db.DropTableIfExists(&TimeSplitInformation{})
		db.DropTableIfExists(&TranscodeTemplateParameter{})
		db.DropTableIfExists(&StorageLocation{})
		db.DropTableIfExists(&ChunkTranscodeProfile{})
		db.DropTableIfExists(&ChunkTranscodeParameter{})
		db.DropTableIfExists(&ChunkError{})
		db.DropTableIfExists(&FileError{})
		db.DropTableIfExists(&TranscodedFile{})
		db.DropTableIfExists(&TranscodedFileMetaData{})
	}
}
