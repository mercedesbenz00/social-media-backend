package iq.earthlink.social.common.filestorage.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "filestorage.minio")
@Component
public class MinioProperties {

  private String endpoint;

  private String externalEndpoint;

  private String accessKey;

  private String secretKey;

  private String bucketName;
}
