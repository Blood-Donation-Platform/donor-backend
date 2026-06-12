package pt.sanguept.donationnotification.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pt.sanguept.donationnotification.entities.NotificationRequest;
import pt.sanguept.donationnotification.repositories.NotificationRequestRepository;
import pt.sanguept.donationsession.entities.DonationSession;
import pt.sanguept.donationsession.events.SessionPublishedEvent;
import pt.sanguept.donationsession.repositories.DonationSessionRepository;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationGenerationService {

    private final DonationSessionRepository sessionRepository;
    private final NotificationMatchingService matchingService;
    private final NotificationRequestRepository requestRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void handle(SessionPublishedEvent event) {
        DonationSession session = sessionRepository.findById(event.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + event.sessionId()));

        Set<UUID> interestedUsers = matchingService.findInterestedUsers(session);

        for (UUID userId : interestedUsers) {
            if (!requestRepository.existsByUserIdAndSessionId(userId, session.getId())) {
                NotificationRequest request = NotificationRequest.builder()
                        .userId(userId)
                        .sessionId(session.getId())
                        .build();
                requestRepository.save(request);
            }
        }
    }

}
