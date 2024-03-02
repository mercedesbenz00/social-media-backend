package filestorage

import (
	"io"
	"log"
	"net/url"
	"path"

	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	"github.com/minio/minio-go"
)

type Controller struct {
	minioClient    *minio.Client
	endPoint       string
	publicEndPoint string
}

func (ceph *Controller) Init(storageSecrets models.StorageSecrets) error {
	var err error
	ceph.endPoint = storageSecrets.EndPoint
	ceph.publicEndPoint = storageSecrets.PublicEndPoint
	ceph.minioClient, err = minio.NewV2(storageSecrets.EndPoint, storageSecrets.AccessKey, storageSecrets.SecretKey, storageSecrets.Secure)
	if err != nil {
		log.Printf("ERROR: Can't create the storage driver - %s", err.Error())
		return err
	}
	log.Println("LOG: Storage driver is up")
	return nil
}

func (ceph *Controller) GetPublicUrl(bucketName string, objectName string) string {
	publicURL, _ := url.Parse(ceph.publicEndPoint)
	publicURL.Path = path.Join(publicURL.Path, bucketName, objectName)
	return publicURL.String()
}

// Generates a presigned URL for HTTP GET operations.
func (ceph *Controller) SignUrl(bucketName string, objectName string) (string, error) {
	reqParams := make(url.Values)
	fileurl, err := ceph.minioClient.PresignedGetObject(bucketName, objectName, models.MINIO_DEFAULT_EXPIRE_PRESIGNED, reqParams)

	if err != nil {
		log.Printf("ERROR: Didn't manage to sign url for %s/%s", bucketName, objectName)
		return "", err
	}

	return fileurl.String(), nil
}

// Downloads and saves the object as a file in the local filesystem.
func (ceph *Controller) DownloadFile(bucketName string, objectName string, base string) error {

	var err = ceph.minioClient.FGetObject(bucketName, objectName, base+"/"+objectName, minio.GetObjectOptions{})
	if err != nil {
		log.Printf("ERROR: Didn't manage to download file %s/%s", bucketName, objectName)
		return err
	}
	return err

}

func (ceph *Controller) checkBuchet(bucketName string) error {
	exists, err := ceph.minioClient.BucketExists(bucketName)
	if err != nil {
		log.Printf("ERROR: Didn't manage to check the bucket %s", bucketName)
		return err
	}

	if !exists {
		if err = ceph.minioClient.MakeBucket(bucketName, ""); err != nil {
			log.Printf("ERROR: Didn't manage to create the bucket %s", bucketName)
			return err
		}
	}
	return nil
}

// UploadData Uploads contents from a reader to object
func (ceph *Controller) UploadData(bucketName string, objectName string, reader io.Reader, dataSize int64, acl string) error {
	if err := ceph.checkBuchet(bucketName); err != nil {
		return err
	}

	putOptions := minio.PutObjectOptions{}
	if acl != "" {
		userMetaData := map[string]string{"x-amz-acl": acl}
		putOptions.UserMetadata = userMetaData
	}

	_, err := ceph.minioClient.PutObject(bucketName, objectName, reader, dataSize, putOptions)
	if err != nil {
		log.Printf("ERROR: Didn't manage to upload data %s/%s", bucketName, objectName)
	}
	return err
}

// Upload Uploads contents from a file to object
func (ceph *Controller) Upload(bucketName string, fileName string, objectName string, acl string) error {
	if err := ceph.checkBuchet(bucketName); err != nil {
		return err
	}

	putOptions := minio.PutObjectOptions{}
	if acl != "" {
		userMetaData := map[string]string{"x-amz-acl": acl}
		putOptions.UserMetadata = userMetaData
	}
	_, err := ceph.minioClient.FPutObject(bucketName, objectName, fileName, putOptions)
	if err != nil {
		log.Printf("ERROR: Didn't manage to upload file %s/%s - %s", bucketName, objectName, err.Error())
		return err
	}
	return err
}
