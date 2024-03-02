package iq.earthlink.social.groupservice.config;

import iq.earthlink.social.groupservice.category.CategoryLocalized;
import org.dozer.DozerConverter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class CategoryLocalizationConverter extends DozerConverter<Map<String, CategoryLocalized>, Map<String, String>> {


    public CategoryLocalizationConverter() {
        super((Class<Map<String, CategoryLocalized>>) (Class<?>) Map.class, (Class<Map<String, String>>) (Class<?>) Map.class);

    }

    @Override
    public Map<String, String> convertTo(Map<String, CategoryLocalized> source, Map<String, String> destination) {
        return Collections.emptyMap();
    }

    @Override
    public Map convertFrom(Map source, Map destination) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> convertedList = new HashMap<>();
        for (Object category : source.values()) {
            if (category instanceof CategoryLocalized categoryLocalized) {
                convertedList.put(categoryLocalized.getLocale(), categoryLocalized.getName());
            }
        }
        return convertedList;
    }
}
