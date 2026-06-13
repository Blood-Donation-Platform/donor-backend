package pt.sanguept.donationnotificationdelivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.sanguept.donationnotification.entities.NotificationRequest;

@Slf4j
@Component
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public void send(NotificationRequest request) {
        log.info("Sending notification to user {} for session {} (request: {})",
                request.getUserId(), request.getSessionId(), request.getId());
    }

}
