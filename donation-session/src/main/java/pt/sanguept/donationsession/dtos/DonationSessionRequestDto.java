package pt.sanguept.donationsession.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record DonationSessionRequestDto(
        @NotBlank String title,
        String description,
        @NotNull UUID locationId,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt
) {}
