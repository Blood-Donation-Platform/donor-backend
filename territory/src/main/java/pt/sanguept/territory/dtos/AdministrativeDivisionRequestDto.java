package pt.sanguept.territory.dtos;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AdministrativeDivisionRequestDto(
        String name,
        UUID parentId,
        Boolean active
) { }
