package pt.sanguept.donationnotification.dtos;

import lombok.Builder;

import java.time.Instant;

@Builder
public record NotificationPreferenceDto(
        boolean enabled,
        Instant muteUntil
) {}
