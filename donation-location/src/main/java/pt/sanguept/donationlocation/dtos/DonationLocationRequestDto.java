package pt.sanguept.donationlocation.dtos;

import lombok.Builder;

@Builder
public record DonationLocationRequestDto(
        String name,
        String address,
        Double latitude,
        Double longitude,
        Long administrativeDivisionId,
        Boolean active,
        String externalId
) {}
