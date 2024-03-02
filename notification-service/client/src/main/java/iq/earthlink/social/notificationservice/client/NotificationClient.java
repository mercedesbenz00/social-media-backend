package iq.earthlink.social.notificationservice.client;

public interface NotificationClient {

    void sendNotification(Long personId, Notification notification) throws NotificationClientException;

}
