package pt.sanguept.territory.dtos;

import lombok.Builder;

@Builder
public record AdministrativeDivisionRequestDto(
        String name,
        Long parentId,
        Boolean active
) { }
