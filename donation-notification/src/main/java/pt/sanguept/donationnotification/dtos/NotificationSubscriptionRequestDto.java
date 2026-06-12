package pt.sanguept.donationnotification.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import pt.sanguept.donationnotification.enums.SubscriptionType;

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
