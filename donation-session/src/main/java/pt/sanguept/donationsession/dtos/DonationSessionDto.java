package pt.sanguept.donationsession.dtos;

import lombok.Builder;
import pt.sanguept.donationsession.enums.SessionStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record DonationSessionDto(
        UUID id,
        String title,
        String description,
        UUID locationId,
        String locationName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        SessionStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
