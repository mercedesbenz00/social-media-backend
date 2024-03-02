package iq.earthlink.social.common.util;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import iq.earthlink.social.exception.RestApiException;
import org.springframework.http.HttpStatus;

import javax.annotation.CheckForNull;

public final class Preconditions {

    private Preconditions() {
        throw new IllegalStateException("Utility class");
    }
    @CanIgnoreReturnValue
    public static <T> T checkNotNull(
            @CheckForNull T obj, String errorMessage, Object... args) {
        if (obj == null) {
            throw new RestApiException(HttpStatus.BAD_REQUEST, errorMessage, args);
        }
        return obj;
    }
}
