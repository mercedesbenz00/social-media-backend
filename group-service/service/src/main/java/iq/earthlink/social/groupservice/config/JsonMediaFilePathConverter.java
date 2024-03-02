package iq.earthlink.social.groupservice.config;

import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import org.dozer.DozerConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class JsonMediaFilePathConverter extends DozerConverter<JsonMediaFile, JsonMediaFile> {

    public JsonMediaFilePathConverter() {
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