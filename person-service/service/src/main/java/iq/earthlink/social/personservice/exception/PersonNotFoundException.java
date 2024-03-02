package iq.earthlink.social.personservice.exception;

public class PersonNotFoundException extends RuntimeException {

  public PersonNotFoundException(String message) {
    super(message);
  }

  public PersonNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
