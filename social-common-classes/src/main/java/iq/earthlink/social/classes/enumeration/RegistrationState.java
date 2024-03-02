package iq.earthlink.social.classes.enumeration;

import lombok.Getter;

import java.util.stream.Stream;

public enum RegistrationState {
    REGISTRATION_COMPLETED("REGISTRATION_COMPLETED"),
    INTERESTS_PROVIDED("INTERESTS_PROVIDED", REGISTRATION_COMPLETED),
    INFO_PROVIDED("INFO_PROVIDED", INTERESTS_PROVIDED),
    EMAIL_CONFIRMED("EMAIL_CONFIRMED", INFO_PROVIDED),
    ACCOUNT_CREATED("ACCOUNT_CREATED", EMAIL_CONFIRMED);

    @Getter
    private final RegistrationState[] nextStates;
    @Getter
    private final String displayName;

    RegistrationState(String displayName, RegistrationState... nextStates) {
        this.nextStates = nextStates;
        this.displayName = displayName;
    }

    public boolean canBeChangedTo(RegistrationState nextState) {
        if (nextState == null || this.equals(nextState)) {
            return false;
        }

        return Stream.of(this.nextStates)
                .anyMatch(s -> s == nextState);
    }
}
