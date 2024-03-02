package iq.earthlink.social.postservice.config;

import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import org.dozer.DozerConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class MediaFilePathConverter extends DozerConverter<JsonMediaFile, JsonMediaFile> {


    public MediaFilePathConverter() {
        super(JsonMediaFile.class, JsonMediaFile.class);
    }

    @Override
    public JsonMediaFile convertTo(JsonMediaFile source, JsonMediaFile destination) {
       return null;
    }

    @Override
    public JsonMediaFile convertFrom(JsonMediaFile source, JsonMediaFile destination) {
        if (ObjectUtils.isEmpty(source)) {
            return null;
        }
        return source;
    }
}
