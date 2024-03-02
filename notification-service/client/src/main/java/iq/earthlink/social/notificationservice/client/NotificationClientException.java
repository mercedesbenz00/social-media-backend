package iq.earthlink.social.notificationservice.client;

public class NotificationClientException extends RuntimeException {
    public NotificationClientException(Exception e) {
        super(e);
    }

    public NotificationClientException(String message, Exception e) {
        super(message, e);
    }
}
