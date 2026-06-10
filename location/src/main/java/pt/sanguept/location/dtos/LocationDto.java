package pt.sanguept.location.dtos;

import lombok.Builder;

import java.util.UUID;

@Builder
public record LocationDto(
        UUID id,
        String name,
        String address,
        Double latitude,
        Double longitude,
        Long administrativeDivisionId,
        String administrativeDivisionName,
        Boolean active,
        String externalId
) {}
