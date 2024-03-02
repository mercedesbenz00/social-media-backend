package iq.earthlink.social.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import iq.earthlink.social.exception.RestApiException;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

public class ExceptionUtil {

    public static final String TIMESTAMP = "timestamp";
    public static final String STATUS = "status";
    public static final String MESSAGE = "message";
    public static final String ERROR = "error";
    public static final String UNAUTHORIZED_REQUEST = "Unauthorized request";

    private ExceptionUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static void processFeignException(FeignException ex, RestApiException localizedException, HttpStatus status) {
        HttpStatus exceptionStatus = ex.status() == 0 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.valueOf(ex.status());
        if (Objects.nonNull(localizedException) && exceptionStatus.equals(status)) {
            throw localizedException;
        } else {
            throw new RestApiException(exceptionStatus, ex.getMessage(), ex);
        }
    }

    public static void processFeignException(FeignException ex) {
        processFeignException(ex, null, null);
    }

    public static void writeExceptionToResponse(HttpServletResponse response, String errorKey, String errorDescription, Integer statusCode) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, new Date().getTime());
        error.put(STATUS, statusCode);
        error.put(MESSAGE, errorDescription);
        error.put(ERROR, Collections.singletonList(errorKey));
        response.setStatus(statusCode);
        response.setContentType(APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(response.getOutputStream(), error);
    }
}
