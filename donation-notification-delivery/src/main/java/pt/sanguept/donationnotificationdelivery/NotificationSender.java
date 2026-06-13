package pt.sanguept.donationnotificationdelivery;

import pt.sanguept.donationnotification.entities.NotificationRequest;

public interface NotificationSender {

    void send(NotificationRequest request);

}
