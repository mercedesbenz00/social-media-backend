package iq.earthlink.social.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class BadRequestException extends RestApiException {

    public BadRequestException(String message, Object... args) {
        super(HttpStatus.BAD_REQUEST, message, args);
    }

    public BadRequestException(String message, List<String> errors, Object... args) {
        super(HttpStatus.BAD_REQUEST, message, errors, args);
    }
}
