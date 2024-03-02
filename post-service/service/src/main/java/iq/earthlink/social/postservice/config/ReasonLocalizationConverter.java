package iq.earthlink.social.postservice.config;

import iq.earthlink.social.postservice.post.complaint.model.ReasonLocalized;
import org.dozer.DozerConverter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReasonLocalizationConverter extends DozerConverter<Map<String, ReasonLocalized>, Map<String, String>> {

    public ReasonLocalizationConverter() {
        super((Class<Map<String, ReasonLocalized>>) (Class<?>) Map.class, (Class<Map<String, String>>) (Class<?>) Map.class);
    }

    @Override
    public Map<String, String> convertTo(Map<String, ReasonLocalized> source, Map<String, String> destination) {
        return Collections.emptyMap();
    }

    @Override
    public Map convertFrom(Map source, Map destination) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> convertedList = new HashMap<>();
        for (Object reason : source.values()) {
            if (reason instanceof ReasonLocalized reasonLocalized) {
                convertedList.put(reasonLocalized.getLocale(), reasonLocalized.getName());
            }
        }
        return convertedList;
    }
}
