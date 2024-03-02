package iq.earthlink.social.classes.data.dto;

import iq.earthlink.social.classes.enumeration.MediaFileType;
import lombok.*;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class JsonMediaFile implements Serializable {
    private Long id;
    private MediaFileType fileType;
    private String path;
    private String mimeType;
    private Long ownerId;
    private Long size;
    private Date createdAt;
    private JsonMediaFileTranscoded transcodedFile;
    private Map<String, List<JsonSizedImage>> sizedImages;
}
