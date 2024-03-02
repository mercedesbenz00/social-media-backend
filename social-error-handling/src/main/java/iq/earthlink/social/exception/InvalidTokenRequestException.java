package iq.earthlink.social.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidTokenRequestException extends RestApiException {

    public InvalidTokenRequestException(String message, Object ...args) {
        super(HttpStatus.UNAUTHORIZED, message, args);
    }
}
