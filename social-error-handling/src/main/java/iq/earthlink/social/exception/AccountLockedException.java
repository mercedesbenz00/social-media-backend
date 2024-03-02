package iq.earthlink.social.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for locked account error (423 http status)
 */
public class AccountLockedException extends RestApiException {

    public AccountLockedException(String message, Object... args) {
        super(HttpStatus.LOCKED, message, args);
    }
}
