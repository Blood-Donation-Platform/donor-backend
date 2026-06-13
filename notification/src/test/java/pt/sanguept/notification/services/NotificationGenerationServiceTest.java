package pt.sanguept.notification.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.sanguept.notification.entities.NotificationRequest;
import pt.sanguept.notification.enums.NotificationRequestStatus;
import pt.sanguept.notification.repositories.NotificationRequestRepository;
import pt.sanguept.donationsession.entities.DonationSession;
import pt.sanguept.donationsession.events.SessionPublishedEvent;
import pt.sanguept.donationsession.repositories.DonationSessionRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationGenerationServiceTest {

    @Mock
    private DonationSessionRepository sessionRepository;

    @Mock
    private NotificationMatchingService matchingService;

    @Mock
    private NotificationRequestRepository requestRepository;

    @InjectMocks
    private NotificationGenerationService generationService;

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final UUID USER_1 = UUID.randomUUID();
    private static final UUID USER_2 = UUID.randomUUID();

    @Test
    void shouldCreateNotificationRequestsForMatchedUsers() {
        var event = new SessionPublishedEvent(SESSION_ID, LOCATION_ID,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(3));
        var session = new DonationSession();
        session.setId(SESSION_ID);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(matchingService.findInterestedUsers(session)).thenReturn(Set.of(USER_1, USER_2));
        when(requestRepository.existsByIdempotencyKey(anyString())).thenReturn(false);

        generationService.handle(event);

        verify(requestRepository, times(2)).save(any(NotificationRequest.class));
    }

    @Test
    void shouldNotCreateDuplicateRequests() {
        var event = new SessionPublishedEvent(SESSION_ID, LOCATION_ID,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(3));
        var session = new DonationSession();
        session.setId(SESSION_ID);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(matchingService.findInterestedUsers(session)).thenReturn(Set.of(USER_1));
        when(requestRepository.existsByIdempotencyKey(anyString())).thenReturn(true);

        generationService.handle(event);

        verify(requestRepository, never()).save(any(NotificationRequest.class));
    }

    @Test
    void shouldCreateNoRequestsWhenNoMatches() {
        var event = new SessionPublishedEvent(SESSION_ID, LOCATION_ID,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(3));
        var session = new DonationSession();
        session.setId(SESSION_ID);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(matchingService.findInterestedUsers(session)).thenReturn(Set.of());

        generationService.handle(event);

        verify(requestRepository, never()).save(any(NotificationRequest.class));
    }

    @Test
    void shouldCreateRequestsWithPendingStatus() {
        var event = new SessionPublishedEvent(SESSION_ID, LOCATION_ID,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(3));
        var session = new DonationSession();
        session.setId(SESSION_ID);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(matchingService.findInterestedUsers(session)).thenReturn(Set.of(USER_1));
        when(requestRepository.existsByIdempotencyKey(anyString())).thenReturn(false);

        generationService.handle(event);

        verify(requestRepository).save(argThat(request ->
                request.getStatus() == NotificationRequestStatus.PENDING
                && request.getUserId().equals(USER_1)
                && request.getSessionId().equals(SESSION_ID)
                && request.getIdempotencyKey() != null
        ));
    }
}
