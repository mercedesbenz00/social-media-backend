package iq.earthlink.social.exception;

import io.sentry.Sentry;
import iq.earthlink.social.util.LocalizationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The global REST Api exception handler. Should be used by all REST Api services.
 */
@ControllerAdvice
public class GlobalRestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalRestExceptionHandler.class);
    private static final String ERROR_INVALID_DATA = "error.invalid.data";

    @Autowired
    private LocalizationUtil localizationUtil;

    public GlobalRestExceptionHandler() {
        LOG.info("GlobalRestExceptionHandler is initialized");
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
                                                             HttpHeaders headers, HttpStatus status, WebRequest request) {
        LOG.error("Exception caught", ex);

        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

        List<String> errors = new ArrayList<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ": " + localizationUtil.getLocalizedMessage(error.getDefaultMessage()));
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(error.getObjectName() + ": " + localizationUtil.getLocalizedMessage(error.getDefaultMessage()));
        }

        final HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
        String message = localizationUtil.getLocalizedMessage(ERROR_INVALID_DATA);
        ApiError apiError = ApiError.builder()
                .status(responseStatus.value())
                .message(message)
                .errors(errors)
                .build();

        return handleExceptionInternal(
                ex, apiError, headers, responseStatus, request);
    }


    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {

        List<String> errors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ": " + localizationUtil.getLocalizedMessage(error.getDefaultMessage()));
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(error.getObjectName() + ": " + localizationUtil.getLocalizedMessage(error.getDefaultMessage()));
        }

        final HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
        String message = localizationUtil.getLocalizedMessage(ERROR_INVALID_DATA);
        ApiError apiError = ApiError.builder()
                .status(responseStatus.value())
                .message(message)
                .errors(errors)
                .build();

        return handleExceptionInternal(
                ex, apiError, headers, responseStatus, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status,
            WebRequest request) {
        String error = ex.getParameterName() + " parameter is missing";
        String message = localizationUtil.getLocalizedMessage("error.required.parameter.not.present", ex.getParameterType(), ex.getParameterName());
        final HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
        ApiError apiError = ApiError.builder()
                .status(responseStatus.value())
                .message(message)
                .errors(Collections.singletonList(error))
                .build();

        return new ResponseEntity<>(
                apiError, new HttpHeaders(), responseStatus);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        final HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
        String message = localizationUtil.getLocalizedMessage(ERROR_INVALID_DATA);
        ApiError apiError = ApiError.builder()
                .status(responseStatus.value())
                .message(message)
                .build();

        return new ResponseEntity<>(
                apiError, new HttpHeaders(), responseStatus);
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        List<String> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.add(violation.getRootBeanClass().getName() + " " +
                    violation.getPropertyPath() + ": " + violation.getMessage());
        }

        final HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
        ApiError apiError = ApiError.builder()
                .status(responseStatus.value())
                .message(localizationUtil.getLocalizedMessage(ERROR_INVALID_DATA))
                .errors(errors)
                .build();
        return new ResponseEntity<>(
                apiError, new HttpHeaders(), responseStatus);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        logException(ex);

        Class<?> type = ex.getRequiredType();
        if (type != null) {
            String error = ex.getName() + " should be of type " + type.getName();

            final HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
            ApiError apiError = ApiError.builder()
                    .status(responseStatus.value())
                    .message(ex.getLocalizedMessage())
                    .errors(Collections.singletonList(error))
                    .build();

            return new ResponseEntity<>(
                    apiError, new HttpHeaders(), responseStatus);
        }
        return null;
    }

    @ExceptionHandler({BadRequestException.class})
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex) {
        return handleCustomApiException(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({InvalidTokenRequestException.class})
    public ResponseEntity<Object> handleInvalidTokenRequestException(InvalidTokenRequestException ex) {
        return handleCustomApiException(ex, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({NotFoundException.class})
    public ResponseEntity<Object> handleBadRequestException(NotFoundException ex) {
        return handleCustomApiException(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({RestApiException.class})
    public ResponseEntity<Object> handleBadRequestException(RestApiException ex) {
        return handleCustomApiException(ex, ex.getStatus());
    }

    @ExceptionHandler({NotUniqueException.class})
    public ResponseEntity<Object> handleNotUniqueException(NotUniqueException ex) {
        return handleCustomApiException(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({ForbiddenException.class})
    public ResponseEntity<Object> handleForbiddenException(ForbiddenException ex) {
        return handleCustomApiException(ex, HttpStatus.FORBIDDEN);
    }


    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<Object> handleBadCredentialException(BadCredentialsException ex) {
        logException(ex);

        final HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
        ApiError apiError = ApiError.builder()
                .status(responseStatus.value())
                .message(localizationUtil.getLocalizedMessage("error.bad.credentials"))
                .errors(Collections.singletonList(ex.getMessage()))
                .build();

        return new ResponseEntity<>(
                apiError, new HttpHeaders(), responseStatus);
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class})
    public ResponseEntity<Object> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        logException(ex);

        final HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
        ApiError apiError = ApiError.builder()
                .status(responseStatus.value())
                .message(localizationUtil.getLocalizedMessage("error.file.too.large"))
                .errors(Collections.singletonList(ex.getMessage()))
                .build();

        return new ResponseEntity<>(
                apiError, new HttpHeaders(), responseStatus);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> anyExceptionHandler(Exception ex) {
        logException(ex);
        Sentry.captureException(ex);

        final ApiError error = ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(localizationUtil.getLocalizedMessage("error.internal.server.error"))
                .errors(Collections.singletonList(getLocalizedMessage(ex)))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private <T extends RestApiException> ResponseEntity<Object> handleCustomApiException(T ex, HttpStatus status) {
        logException(ex);
        List<String> errors;
        if(Objects.nonNull(ex.getErrors()))
            errors = ex.getErrors();
        else
            errors = Collections.singletonList(getLocalizedMessage(ex));

        ApiError.ApiErrorBuilder builder = ApiError.builder()
                .status(status.value())
                .message(getLocalizedMessage(ex))
                .errors(errors);

        ApiError error = builder.build();

        return new ResponseEntity<>(
                error, new HttpHeaders(), status);
    }

    protected String getLocalizedMessage(Exception ex) {
        String msg = ex.getMessage();

        if (ex instanceof HasArguments hasArguments) {
            msg = localizationUtil.getLocalizedMessage(msg, hasArguments.getArgs());
        } else {
            msg = localizationUtil.getLocalizedMessage(msg);
        }

        return msg;
    }

    private <T extends Throwable> void logException(T ex) {
        LOG.error("Caught api exception", ex);
    }
}
