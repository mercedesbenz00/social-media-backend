package iq.earthlink.social.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends RestApiException {

    public ForbiddenException(String message, Object ...args) {
        super(HttpStatus.FORBIDDEN, message, args);
    }
}
