package pt.sanguept.donationnotification.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public void send(UUID requestId, UUID userId, UUID sessionId) {
        log.info("Sending notification to user {} for session {} (request: {})", userId, sessionId, requestId);
    }

}
