package pt.sanguept.donationnotification.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.donationnotification.entities.NotificationRequest;
import pt.sanguept.donationnotification.enums.NotificationRequestStatus;
import pt.sanguept.donationnotification.repositories.NotificationRequestRepository;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessor {

    private final NotificationRequestRepository requestRepository;
    private final NotificationSender sender;

    @Value("${app.notification.processing.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.notification.processing.interval:30000}")
    @Transactional
    public void processPendingRequests() {
        var pending = requestRepository.findPendingForProcessing(
                NotificationRequestStatus.PENDING.name(), batchSize);

        if (pending.isEmpty()) {
            log.debug("No pending notification requests");
            return;
        }

        log.info("Processing {} pending notification requests", pending.size());

        int succeeded = 0;
        int failed = 0;

        for (NotificationRequest request : pending) {
            request.setStatus(NotificationRequestStatus.PROCESSING);
            request.setAttemptCount(request.getAttemptCount() + 1);
            request.setLastAttemptAt(Instant.now());
            requestRepository.save(request);

            try {
                sender.send(request.getUserId(), request.getSessionId());

                request.setStatus(NotificationRequestStatus.PROCESSED);
                request.setProcessedAt(Instant.now());
                requestRepository.save(request);
                succeeded++;

                log.info("Notification processed for user {} session {}", request.getUserId(), request.getSessionId());
            } catch (Exception e) {
                handleFailure(request, e);
                failed++;
            }
        }

        log.info("Batch complete: {} processed, {} succeeded, {} failed", pending.size(), succeeded, failed);
    }

    private void handleFailure(NotificationRequest request, Exception e) {
        log.error("Failed to send notification for user {} session {}: {}",
                request.getUserId(), request.getSessionId(), e.getMessage());

        request.setFailureReason(e.getMessage());
        requestRepository.save(request);

        if (request.hasExceededMaxAttempts()) {
            request.setStatus(NotificationRequestStatus.FAILED);
            log.warn("Notification permanently failed for user {} session {} after {} attempts",
                    request.getUserId(), request.getSessionId(), request.getAttemptCount());
        } else {
            request.setStatus(NotificationRequestStatus.PENDING);
        }

        requestRepository.save(request);
    }

}
