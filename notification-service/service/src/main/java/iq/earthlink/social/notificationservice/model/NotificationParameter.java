package iq.earthlink.social.notificationservice.model;


public enum NotificationParameter {
    SOUND("default"),
    COLOR("#FFFF00");

    private final String value;

    NotificationParameter(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
