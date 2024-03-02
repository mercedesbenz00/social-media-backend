package iq.earthlink.social.classes.data.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentModerationDto implements Serializable {
    @JsonProperty("type")
    private String type;
    @JsonProperty("id")
    private Long id; //commentId/postId
    @JsonProperty("aiModel")
    private String aiModel; //"offensiveLang"
    @JsonProperty("analyzedAt")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date analyzedAt; //'%Y-%m-%d %H:%M:%S'
    @JsonProperty("reason_key")
    private String reasonKey; //0000
    @JsonProperty("action")
    private String action; // "delete"

    public ContentModerationDto(String type, Long id) {
        this.type = type;
        this.id = id;
    }
}
