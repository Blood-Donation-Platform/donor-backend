package pt.sanguept.donationsession.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pt.sanguept.donationlocation.entities.DonationLocation;
import pt.sanguept.donationsession.dtos.DonationSessionRequestDto;
import pt.sanguept.donationsession.entities.DonationSession;
import pt.sanguept.donationsession.enums.SessionStatus;
import pt.sanguept.donationsession.events.SessionCancelledEvent;
import pt.sanguept.donationsession.events.SessionCompletedEvent;
import pt.sanguept.donationsession.events.SessionPublishedEvent;
import pt.sanguept.donationsession.repositories.DonationSessionRepository;
import pt.sanguept.donationlocation.repositories.DonationLocationRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonationSessionServiceTest {

    @Mock
    private DonationSessionRepository repository;

    @Mock
    private DonationLocationRepository donationLocationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Clock clock;

    @InjectMocks
    private DonationSessionService service;

    private static final Instant FIXED_NOW = Instant.parse("2026-06-15T12:00:00Z");
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final DonationLocation ACTIVE_LOCATION = buildLocation(true);
    private static final DonationLocation INACTIVE_LOCATION = buildLocation(false);

    @BeforeEach
    void setUpClock() {
        lenient().when(clock.instant()).thenReturn(FIXED_NOW);
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    // ── Publish flow ──────────────────────────────────────────

    @Test
    void shouldCreateDraftSession() {
        var dto = validRequest();
        when(donationLocationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(ACTIVE_LOCATION));
        when(repository.save(any())).thenAnswer(inv -> {
            DonationSession s = inv.getArgument(0);
            s.setId(SESSION_ID);
            return s;
        });

        var result = service.createSession(dto);

        assertThat(result.getSessionStatus()).isEqualTo(SessionStatus.DRAFT);
        assertThat(result.getTitle()).isEqualTo("Blood Drive Spring 2026");
        assertThat(result.getLocation()).isEqualTo(ACTIVE_LOCATION);
    }

    @Test
    void shouldPublishDraftSession() {
        var session = draftSession(futureStart());
        when(repository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(repository.save(any())).thenReturn(session);

        var result = service.publishSession(SESSION_ID);

        assertThat(result.getSessionStatus()).isEqualTo(SessionStatus.PUBLISHED);

        var captor = ArgumentCaptor.forClass(SessionPublishedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().sessionId()).isEqualTo(SESSION_ID);
        assertThat(captor.getValue().locationId()).isEqualTo(LOCATION_ID);
    }

    @Test
    void shouldThrowWhenPublishingNonDraftSession() {
        var session = draftSession(futureStart());
        session.setSessionStatus(SessionStatus.CANCELLED);
        when(repository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.publishSession(SESSION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only DRAFT sessions can be published");
    }

    @Test
    void shouldThrowWhenPublishingWithPastStart() {
        var pastStart = LocalDateTime.now(clock).minusDays(1);
        var session = draftSession(pastStart);
        when(repository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.publishSession(SESSION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must start in the future");
    }

    @Test
    void shouldThrowWhenPublishingOnInactiveLocation() {
        var session = draftSession(futureStart());
        session.setLocation(INACTIVE_LOCATION);
        when(repository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.publishSession(SESSION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DonationLocation is not active");
    }

    @Test
    void shouldThrowWhenPublishingWithInvalidTimeRange() {
        var start = futureStart();
        var session = draftSession(start);
        session.setEndAt(start.minusHours(3));
        when(repository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.publishSession(SESSION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startAt must be before endAt");
    }

    // ── Cancel flow ───────────────────────────────────────────

    @Test
    void shouldCancelDraftSession() {
        var session = draftSession(futureStart());
        when(repository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(repository.save(any())).thenReturn(session);

        var result = service.cancelSession(SESSION_ID);

        assertThat(result.getSessionStatus()).isEqualTo(SessionStatus.CANCELLED);

        var captor = ArgumentCaptor.forClass(SessionCancelledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().sessionId()).isEqualTo(SESSION_ID);
    }

    @Test
    void shouldCancelPublishedSession() {
        var session = draftSession(futureStart());
        session.setSessionStatus(SessionStatus.PUBLISHED);
        when(repository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(repository.save(any())).thenReturn(session);

        var result = service.cancelSession(SESSION_ID);

        assertThat(result.getSessionStatus()).isEqualTo(SessionStatus.CANCELLED);

        verify(eventPublisher).publishEvent(any(SessionCancelledEvent.class));
    }

    @Test
    void shouldThrowWhenCancellingCompletedSession() {
        var session = draftSession(futureStart());
        session.setSessionStatus(SessionStatus.COMPLETED);
        when(repository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.cancelSession(SESSION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only DRAFT or PUBLISHED sessions can be cancelled");
    }

    // ── Invalid dates ─────────────────────────────────────────

    @Test
    void shouldThrowWhenStartAfterEnd() {
        when(donationLocationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(ACTIVE_LOCATION));

        var dto = DonationSessionRequestDto.builder()
                .title("Bad Session")
                .locationId(LOCATION_ID)
                .startAt(LocalDateTime.of(2026, 6, 10, 18, 0))
                .endAt(LocalDateTime.of(2026, 6, 10, 9, 0))
                .build();

        assertThatThrownBy(() -> service.createSession(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startAt must be before endAt");
    }

    @Test
    void shouldThrowWhenStartEqualsEnd() {
        when(donationLocationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(ACTIVE_LOCATION));

        var sameTime = LocalDateTime.of(2026, 6, 10, 12, 0);
        var dto = DonationSessionRequestDto.builder()
                .title("Equal Times")
                .locationId(LOCATION_ID)
                .startAt(sameTime)
                .endAt(sameTime)
                .build();

        assertThatThrownBy(() -> service.createSession(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startAt must be before endAt");
    }

    // ── Update rules ──────────────────────────────────────────

    @Test
    void shouldNotUpdatePublishedSession() {
        var session = draftSession(futureStart());
        session.setSessionStatus(SessionStatus.PUBLISHED);
        when(repository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        var dto = DonationSessionRequestDto.builder()
                .title("Hacked Title")
                .build();

        assertThatThrownBy(() -> service.updateSession(SESSION_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only DRAFT sessions can be updated");
    }

    @Test
    void shouldThrowWhenUpdateCreatesInvalidTimeRange() {
        var session = draftSession(futureStart());
        when(repository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        var dto = DonationSessionRequestDto.builder()
                .endAt(futureStart().minusHours(3))
                .build();

        assertThatThrownBy(() -> service.updateSession(SESSION_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startAt must be before endAt");
    }

    // ── Complete expired ──────────────────────────────────────

    @Test
    void shouldCompleteExpiredSessions() {
        var startPast = LocalDateTime.now(clock).minusDays(10);
        var endPast = LocalDateTime.now(clock).minusDays(5);
        var session = draftSession(startPast);
        session.setEndAt(endPast);
        session.setSessionStatus(SessionStatus.PUBLISHED);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(java.util.List.of(session));
        when(repository.save(any())).thenReturn(session);

        service.completeExpiredSessions();

        assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED);

        verify(eventPublisher).publishEvent(any(SessionCompletedEvent.class));
    }

    @Test
    void shouldNotCompleteActiveFutureSession() {
        var session = draftSession(futureStart());
        session.setEndAt(futureStart().plusHours(3));
        session.setSessionStatus(SessionStatus.PUBLISHED);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(java.util.List.of());

        service.completeExpiredSessions();

        assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.PUBLISHED);
    }

    // ── Helpers ───────────────────────────────────────────────

    private static DonationLocation buildLocation(boolean active) {
        DonationLocation location = new DonationLocation();
        location.setId(LOCATION_ID);
        location.setName("Centro de Saude");
        location.setAddress("Rua Principal");
        location.setActive(active);
        return location;
    }

    private DonationSession draftSession(LocalDateTime start) {
        DonationSession session = new DonationSession();
        session.setId(SESSION_ID);
        session.setTitle("Test Session");
        session.setLocation(ACTIVE_LOCATION);
        session.setStartAt(start);
        session.setEndAt(start.plusHours(3));
        session.setSessionStatus(SessionStatus.DRAFT);
        return session;
    }

    private DonationSessionRequestDto validRequest() {
        var start = futureStart();
        return DonationSessionRequestDto.builder()
                .title("Blood Drive Spring 2026")
                .description("Annual blood donation event")
                .locationId(LOCATION_ID)
                .startAt(start)
                .endAt(start.plusHours(4))
                .build();
    }

    private LocalDateTime futureStart() {
        return LocalDateTime.now(clock).plusDays(30).withHour(9).withMinute(0).withSecond(0).withNano(0);
    }
}
