package pt.sanguept.notification.dtos;

import lombok.Builder;

import java.time.Instant;

@Builder
public record NotificationPreferenceDto(
        boolean enabled,
        Instant muteUntil
) {}
