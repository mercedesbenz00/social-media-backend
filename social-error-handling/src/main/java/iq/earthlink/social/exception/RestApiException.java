package iq.earthlink.social.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class RestApiException extends RuntimeException implements HasArguments {

    private final HttpStatus status;

    private final List<String> errors;

    @Getter
    private final Object[] args;

    public RestApiException(HttpStatus status, String message, Exception ex, Object... args) {
        super(message, ex);
        this.status = status;
        this.args = args;
        this.errors = null;
    }

    public RestApiException(HttpStatus status, String message, Object... args) {
        super(message);
        this.status = status;
        this.args = args;
        this.errors = null;
    }

    public RestApiException(HttpStatus status, String message, List<String> errors, Object... args) {
        super(message);
        this.status = status;
        this.args = args;
        this.errors = errors;
    }
}
