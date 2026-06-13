package pt.sanguept.notification.dtos;

import lombok.Builder;
import pt.sanguept.notification.enums.NotificationRequestStatus;

import java.time.Instant;
import java.util.UUID;

@Builder
public record NotificationRequestDto(
        UUID id,
        UUID userId,
        UUID sessionId,
        NotificationRequestStatus status,
        Instant createdAt,
        Instant processedAt,
        int attemptCount,
        Instant lastAttemptAt,
        String failureReason
) {}
