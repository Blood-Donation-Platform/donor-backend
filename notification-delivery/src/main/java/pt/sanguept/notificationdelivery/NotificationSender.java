package pt.sanguept.notificationdelivery;

import pt.sanguept.notification.entities.NotificationRequest;

public interface NotificationSender {

    void send(NotificationRequest request);

}
