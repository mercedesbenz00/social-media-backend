package iq.earthlink.social.personservice.dto;

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
public class MatrixPushTokenDTO {
    private String appDisplayName;
    private String appId;
    private boolean append;
    private PusherDataDTO data;
    private String deviceDisplayName;
    private String kind;
    private String lang;
    private String profile;
    private String pushkey;


}
