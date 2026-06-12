package pt.sanguept.donationnotification.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pt.sanguept.donationnotification.entities.NotificationRequest;
import pt.sanguept.donationnotification.repositories.NotificationRequestRepository;
import pt.sanguept.donationsession.entities.DonationSession;
import pt.sanguept.donationsession.events.SessionPublishedEvent;
import pt.sanguept.donationsession.repositories.DonationSessionRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationGenerationService {

    private final DonationSessionRepository sessionRepository;
    private final NotificationMatchingService matchingService;
    private final NotificationRequestRepository requestRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(SessionPublishedEvent event) {
        DonationSession session = sessionRepository.findById(event.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + event.sessionId()));

        Set<UUID> interestedUsers = matchingService.findInterestedUsers(session);

        for (UUID userId : interestedUsers) {
            String idempotencyKey = computeIdempotencyKey(session.getId(), userId);
            if (!requestRepository.existsByIdempotencyKey(idempotencyKey)) {
                NotificationRequest request = NotificationRequest.builder()
                        .userId(userId)
                        .sessionId(session.getId())
                        .idempotencyKey(idempotencyKey)
                        .build();
                requestRepository.save(request);
            }
        }
    }

    private static String computeIdempotencyKey(UUID sessionId, UUID userId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    (sessionId.toString() + ":" + userId.toString()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

}
