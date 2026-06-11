package pt.sanguept.donationlocation.dtos;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DonationLocationRequestDto(
        String name,
        String address,
        Double latitude,
        Double longitude,
        UUID administrativeDivisionId,
        Boolean active,
        String externalId
) {}
