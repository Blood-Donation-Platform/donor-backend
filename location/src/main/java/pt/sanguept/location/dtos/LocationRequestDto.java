package pt.sanguept.location.dtos;

import lombok.Builder;

@Builder
public record LocationRequestDto(
        String name,
        String address,
        Double latitude,
        Double longitude,
        Long administrativeDivisionId,
        Boolean active,
        String externalId
) {}
