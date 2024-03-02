package iq.earthlink.social.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for not found error (404 http error)
 */
public class NotFoundException extends RestApiException {

    public NotFoundException(String message, Object... args) {
        super(HttpStatus.NOT_FOUND, message, args);
    }

    public NotFoundException(Class<?> model, Object id) {
        this(String.format("Cannot find model %s by id = %s", model.getSimpleName(), id));
    }
}
