package pt.sanguept.notification.dtos;

import lombok.Builder;
import pt.sanguept.notification.enums.SubscriptionType;

import java.time.Instant;
import java.util.UUID;

@Builder
public record NotificationSubscriptionDto(
        UUID id,
        UUID userId,
        SubscriptionType type,
        boolean enabled,
        UUID administrativeDivisionId,
        Double latitude,
        Double longitude,
        Integer radiusKm,
        Instant createdAt
) {}
