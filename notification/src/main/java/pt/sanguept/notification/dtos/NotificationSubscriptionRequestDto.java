package pt.sanguept.notification.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import pt.sanguept.notification.enums.SubscriptionType;

import java.util.UUID;

@Builder
public record NotificationSubscriptionRequestDto(
        @NotNull SubscriptionType type,
        boolean enabled,
        UUID administrativeDivisionId,
        Double latitude,
        Double longitude,
        Integer radiusKm
) {}
