package iq.earthlink.social.classes.data.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class JsonStorageDetails {

    String storageUrl;
    String bucketName;
    String objectName;
    String accessKeyId;
    String secretAccessKey;
    boolean useSsl;
}
