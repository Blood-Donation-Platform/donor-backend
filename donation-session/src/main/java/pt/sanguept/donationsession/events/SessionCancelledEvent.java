package pt.sanguept.donationsession.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionCancelledEvent(
        UUID sessionId,
        UUID locationId,
        LocalDateTime startAt,
        LocalDateTime endAt
) {}
