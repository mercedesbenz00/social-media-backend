package iq.earthlink.social.classes.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarDetails {
    private String path;
    private String mimeType;
    private Long size;
    private Map<String, List<JsonSizedImage>> sizedImages;
}
