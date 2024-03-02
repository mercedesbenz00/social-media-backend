package iq.earthlink.social.classes.data.dto;

import iq.earthlink.social.classes.enumeration.MediaFileType;
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
public class JsonMediaFileTranscoded implements Serializable {
  private Long id;
  private MediaFileType fileType;
  private String path;
  private String mimeType;
  private Long ownerId;
  private Long size;
  private Date createdAt;
}
