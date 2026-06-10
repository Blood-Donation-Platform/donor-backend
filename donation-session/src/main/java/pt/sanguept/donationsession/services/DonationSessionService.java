package pt.sanguept.donationsession.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.donationsession.dtos.DonationSessionFilter;
import pt.sanguept.donationsession.dtos.DonationSessionRequestDto;
import pt.sanguept.donationsession.entities.DonationSession;
import pt.sanguept.donationsession.enums.SessionStatus;
import pt.sanguept.donationsession.events.SessionCancelledEvent;
import pt.sanguept.donationsession.events.SessionCompletedEvent;
import pt.sanguept.donationsession.events.SessionPublishedEvent;
import pt.sanguept.donationsession.repositories.DonationSessionRepository;
import pt.sanguept.location.repositories.LocationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonationSessionService {

    private final DonationSessionRepository repository;
    private final LocationRepository locationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DonationSession createSession(DonationSessionRequestDto dto) {
        var location = locationRepository.findById(dto.locationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + dto.locationId()));
        if (!location.getActive()) {
            throw new IllegalArgumentException("Location is not active: " + dto.locationId());
        }
        if (!dto.startAt().isBefore(dto.endAt())) {
            throw new IllegalArgumentException("startAt must be before endAt");
        }

        DonationSession session = new DonationSession();
        session.setTitle(dto.title());
        session.setDescription(dto.description());
        session.setLocation(location);
        session.setStartAt(dto.startAt());
        session.setEndAt(dto.endAt());
        session.setSessionStatus(SessionStatus.DRAFT);

        return repository.save(session);
    }

    @Transactional
    public DonationSession updateSession(UUID id, DonationSessionRequestDto dto) {
        DonationSession session = getById(id);

        if (session.getSessionStatus() != SessionStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT sessions can be updated");
        }

        if (dto.title() != null) {
            session.setTitle(dto.title());
        }
        if (dto.description() != null) {
            session.setDescription(dto.description());
        }
        if (dto.locationId() != null) {
            var location = locationRepository.findById(dto.locationId())
                    .orElseThrow(() -> new IllegalArgumentException("Location not found: " + dto.locationId()));
            if (!location.getActive()) {
                throw new IllegalArgumentException("Location is not active: " + dto.locationId());
            }
            session.setLocation(location);
        }
        if (dto.startAt() != null) {
            session.setStartAt(dto.startAt());
        }
        if (dto.endAt() != null) {
            session.setEndAt(dto.endAt());
        }

        if (!session.getStartAt().isBefore(session.getEndAt())) {
            throw new IllegalArgumentException("startAt must be before endAt");
        }

        return repository.save(session);
    }

    public DonationSession getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Donation session not found: " + id));
    }

    public Page<DonationSession> search(DonationSessionFilter filter, Pageable pageable) {
        var spec = locationIdEq(filter.locationId())
                .and(sessionStatusEq(filter.status()))
                .and(startsAfter(filter.startsAfter()))
                .and(endsBefore(filter.endsBefore()));
        return repository.findAll(spec, pageable);
    }

    @Transactional
    public DonationSession publishSession(UUID id) {
        DonationSession session = getById(id);

        if (session.getSessionStatus() != SessionStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT sessions can be published");
        }
        if (!session.getStartAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Published sessions must start in the future");
        }
        if (!session.getStartAt().isBefore(session.getEndAt())) {
            throw new IllegalArgumentException("startAt must be before endAt");
        }
        if (!session.getLocation().getActive()) {
            throw new IllegalArgumentException("Location is not active");
        }

        session.setSessionStatus(SessionStatus.PUBLISHED);
        var saved = repository.save(session);

        eventPublisher.publishEvent(new SessionPublishedEvent(
                saved.getId(), saved.getLocation().getId(), saved.getStartAt(), saved.getEndAt()));

        return saved;
    }

    @Transactional
    public DonationSession cancelSession(UUID id) {
        DonationSession session = getById(id);

        if (session.getSessionStatus() != SessionStatus.DRAFT
                && session.getSessionStatus() != SessionStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only DRAFT or PUBLISHED sessions can be cancelled");
        }

        session.setSessionStatus(SessionStatus.CANCELLED);
        var saved = repository.save(session);

        eventPublisher.publishEvent(new SessionCancelledEvent(
                saved.getId(), saved.getLocation().getId(), saved.getStartAt(), saved.getEndAt()));

        return saved;
    }

    @Transactional
    public void completeExpiredSessions() {
        var now = LocalDateTime.now();
        var spec = sessionStatusEq(SessionStatus.PUBLISHED)
                .and(endAtBefore(now));

        var expired = repository.findAll(spec);
        for (var session : expired) {
            session.setSessionStatus(SessionStatus.COMPLETED);
            repository.save(session);
            eventPublisher.publishEvent(new SessionCompletedEvent(
                    session.getId(), session.getLocation().getId(), session.getStartAt(), session.getEndAt()));
        }
    }

    static Specification<DonationSession> locationIdEq(UUID locationId) {
        return (root, cq, cb) ->
                locationId == null ? cb.conjunction() : cb.equal(root.get("location").get("id"), locationId);
    }

    static Specification<DonationSession> sessionStatusEq(SessionStatus status) {
        return (root, cq, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("sessionStatus"), status);
    }

    static Specification<DonationSession> startsAfter(LocalDate date) {
        return (root, cq, cb) ->
                date == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("startAt"), date.atStartOfDay());
    }

    static Specification<DonationSession> endsBefore(LocalDate date) {
        return (root, cq, cb) ->
                date == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("endAt"), date.atTime(LocalTime.MAX));
    }

    private static Specification<DonationSession> endAtBefore(LocalDateTime dateTime) {
        return (root, cq, cb) -> cb.lessThan(root.get("endAt"), dateTime);
    }
}
