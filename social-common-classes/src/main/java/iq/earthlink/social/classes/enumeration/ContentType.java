package iq.earthlink.social.classes.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ContentType {
    POST("post"),
    POST_COMMENT("comment"),
    STORY("story"),
    GROUP("group"),
    PERSON("person");

    private static final Map<String, ContentType> displayNameMap = Stream
            .of(ContentType.values())
            .collect(Collectors.toMap(s -> s.displayName, Function.identity()));
    @Getter
    private final String displayName;

    ContentType(String displayName) {
        this.displayName = displayName;
    }

    @JsonCreator
    public static ContentType fromString(String displayName) {
        return Optional
                .ofNullable(displayNameMap.get(displayName))
                .orElseThrow(() -> new IllegalArgumentException(displayName));
    }
}
