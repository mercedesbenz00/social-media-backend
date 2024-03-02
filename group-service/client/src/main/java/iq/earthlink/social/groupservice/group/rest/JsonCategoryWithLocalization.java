package iq.earthlink.social.groupservice.group.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;


@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class JsonCategoryWithLocalization extends  JsonCategory {

    private Map<String, String> localizations = new HashMap<>();
    private JsonCategoryWithLocalization parentCategory;

}
