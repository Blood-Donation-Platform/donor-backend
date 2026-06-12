package pt.sanguept.donationnotification.services;

import java.util.UUID;

public interface NotificationSender {

    void send(UUID userId, UUID sessionId);

}
