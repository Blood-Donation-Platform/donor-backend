package pt.sanguept.donationnotification.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import pt.sanguept.donationnotification.entities.NotificationRequest;
import pt.sanguept.donationnotification.enums.NotificationRequestStatus;
import pt.sanguept.donationnotification.repositories.NotificationRequestRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationProcessorTest {

    @Mock
    private NotificationRequestRepository requestRepository;

    @Mock
    private NotificationSender sender;

    @InjectMocks
    private NotificationProcessor processor;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @Test
    void shouldProcessPendingRequestSuccessfully() {
        var request = pendingRequest();
        when(requestRepository.findPending(eq(NotificationRequestStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(request));

        processor.processPendingRequests();

        assertThat(request.getStatus()).isEqualTo(NotificationRequestStatus.PROCESSED);
        assertThat(request.getAttemptCount()).isEqualTo(1);
        assertThat(request.getProcessedAt()).isNotNull();
        verify(sender).send(USER_ID, SESSION_ID);
    }

    @Test
    void shouldSetProcessingStatusBeforeSending() {
        var request = pendingRequest();
        when(requestRepository.findPending(eq(NotificationRequestStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(request));

        processor.processPendingRequests();

        verify(requestRepository, times(2)).save(any(NotificationRequest.class));
        assertThat(request.getStatus()).isEqualTo(NotificationRequestStatus.PROCESSED);
    }

    @Test
    void shouldRetryOnFailureAndGoBackToPending() {
        var request = pendingRequest();
        when(requestRepository.findPending(eq(NotificationRequestStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(request));
        doThrow(new RuntimeException("Connection refused")).when(sender).send(USER_ID, SESSION_ID);

        processor.processPendingRequests();

        assertThat(request.getStatus()).isEqualTo(NotificationRequestStatus.PENDING);
        assertThat(request.getAttemptCount()).isEqualTo(1);
        assertThat(request.getFailureReason()).isEqualTo("Connection refused");
        assertThat(request.getLastAttemptAt()).isNotNull();
    }

    @Test
    void shouldSetFailedAfterMaxAttempts() {
        var request = pendingRequest();
        request.setAttemptCount(2);
        when(requestRepository.findPending(eq(NotificationRequestStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(request));
        doThrow(new RuntimeException("Connection refused")).when(sender).send(USER_ID, SESSION_ID);

        processor.processPendingRequests();

        assertThat(request.getStatus()).isEqualTo(NotificationRequestStatus.FAILED);
        assertThat(request.getAttemptCount()).isEqualTo(3);
        assertThat(request.hasExceededMaxAttempts()).isTrue();
    }

    @Test
    void shouldDoNothingWhenNoPendingRequests() {
        when(requestRepository.findPending(eq(NotificationRequestStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of());

        processor.processPendingRequests();

        verify(sender, never()).send(any(), any());
        verify(requestRepository, never()).save(any());
    }

    private NotificationRequest pendingRequest() {
        var request = new NotificationRequest();
        request.setId(UUID.randomUUID());
        request.setUserId(USER_ID);
        request.setSessionId(SESSION_ID);
        request.setStatus(NotificationRequestStatus.PENDING);
        request.setAttemptCount(0);
        return request;
    }
}
