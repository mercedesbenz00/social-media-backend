package iq.earthlink.social.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NotUniqueException extends RestApiException{

    public NotUniqueException(String message, Object ...args) {
        super(HttpStatus.CONFLICT, message, args);
    }

    public NotUniqueException(Exception ex, Object ...args) {
        super(HttpStatus.CONFLICT, ex.getMessage(), args);
    }
}
