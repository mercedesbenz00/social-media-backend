package iq.earthlink.social.exception;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * The common REST Api exception error object.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
  private final int status;
  private final String message;
  private final List<String> errors;
}
