package iq.earthlink.social.classes.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonSizedImage implements Serializable {
    private ImageSizeType imageSizeType;
    private String path;
    private String mimeType;
    private Date createdAt;
    private long size;
}
