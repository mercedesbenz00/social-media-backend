package iq.earthlink.social.classes.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.stream.Stream;

import static com.vladmihalcea.hibernate.util.LogUtils.LOGGER;

public enum PrivacyLevel {
    PUBLIC,
    SELECTED_GROUPS,
    SELECTED_USERS,
    FOLLOWERS;

    @JsonCreator
    public static PrivacyLevel forValue(String value) {
        return Stream.of(PrivacyLevel.values())
                .filter(enumValue -> enumValue.name().equals(value.toUpperCase()))
                .findFirst()
                .orElseGet(() -> getDefaultValue(value));
    }

    private static PrivacyLevel getDefaultValue(String value) {
        LOGGER.info("Wrong 'privacyLevel' value is provided: '{}'. Falling back to default value. Valid values are:  PUBLIC (default), SELECTED_GROUPS, SELECTED_USERS, or FOLLOWERS.", value);
        return PrivacyLevel.PUBLIC;
    }
}
