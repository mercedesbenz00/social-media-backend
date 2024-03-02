package iq.earthlink.social.classes.enumeration;

import java.util.stream.Stream;

public enum EmailType {
    EMAIL_CONFIRMATION("Email Confirmation", "email-confirmation"),
    RESET_PASSWORD("Reset password", "reset-password"),
    INVITE_FRIEND("Invite friend", "invite-friend"),
    CHANGE_EMAIL_REQUEST("Change email request", "change-email-request"),
    UPDATE_EMAIL("Update email", "update-email");

    private final String code;
    private final String title;

    EmailType(String title, String code) {
        this.code = code;
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getCode() {
        return code;
    }

    public static EmailType fromCode(String code) {

        return Stream.of(EmailType.values())
                .filter(c -> c.getCode().equals(code))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
