package pt.sanguept.donationlocation.dtos;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DonationLocationDto(
        UUID id,
        String name,
        String address,
        Double latitude,
        Double longitude,
        UUID administrativeDivisionId,
        String administrativeDivisionName,
        Boolean active,
        String externalId
) {}
