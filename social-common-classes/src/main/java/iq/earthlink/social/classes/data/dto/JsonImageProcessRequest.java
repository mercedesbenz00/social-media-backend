package iq.earthlink.social.classes.data.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class JsonImageProcessRequest {

    String entityId;
    String parentEntityId;
    Boolean isFromBucket;
    String fileUrl;
    String serviceName;
    JsonStorageDetails fromBucket;
    JsonStorageDetails toBucket;
    List<JsonImageParameters> imageProcessParams;
}
