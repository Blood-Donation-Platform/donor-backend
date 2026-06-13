package pt.sanguept.donationnotificationdelivery;

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

    private static final int STUCK_TIMEOUT_MINUTES = 5;

    private final NotificationRequestRepository requestRepository;
    private final NotificationSender sender;

    @Value("${app.notification.processing.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.notification.processing.interval:30000}")
    @Transactional
    public void processPendingRequests() {
        recoverStuckProcessing();

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
            request.setNextAttemptAt(null);
            requestRepository.save(request);

            try {
                sender.send(request);

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

        long queueDepth = requestRepository.countByStatus(NotificationRequestStatus.PENDING);
        long processingCount = requestRepository.countByStatus(NotificationRequestStatus.PROCESSING);
        long failedCount = requestRepository.countByStatus(NotificationRequestStatus.FAILED);

        log.info("Batch complete: {} processed, {} succeeded, {} failed | queue=PENDING:{} PROCESSING:{} FAILED:{}",
                pending.size(), succeeded, failed, queueDepth, processingCount, failedCount);
    }

    private void recoverStuckProcessing() {
        var stuck = requestRepository.findStuckProcessing(STUCK_TIMEOUT_MINUTES);
        if (!stuck.isEmpty()) {
            log.warn("Recovering {} stuck PROCESSING requests", stuck.size());
            for (NotificationRequest request : stuck) {
                request.setStatus(NotificationRequestStatus.PENDING);
                request.setNextAttemptAt(null);
                requestRepository.save(request);
            }
        }
    }

    private void handleFailure(NotificationRequest request, Exception e) {
        log.error("Failed to send notification for user {} session {}: {}",
                request.getUserId(), request.getSessionId(), e.getMessage());

        request.setFailureReason(e.getMessage());
        requestRepository.save(request);

        if (request.hasExceededMaxAttempts()) {
            request.setStatus(NotificationRequestStatus.FAILED);
            request.setNextAttemptAt(null);
            log.warn("Notification permanently failed for user {} session {} after {} attempts",
                    request.getUserId(), request.getSessionId(), request.getAttemptCount());
        } else {
            request.setStatus(NotificationRequestStatus.PENDING);
            request.computeNextAttemptAt();
            log.info("Notification retry scheduled for user {} session {} (attempt {}/{}, next at {})",
                    request.getUserId(), request.getSessionId(),
                    request.getAttemptCount(), 3, request.getNextAttemptAt());
        }

        requestRepository.save(request);
    }

}
