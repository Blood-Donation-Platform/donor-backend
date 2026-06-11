package pt.sanguept.territory.dtos;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AdministrativeDivisionDto(
    UUID id,
    String name,
    UUID parentId,
    Integer monumentsCount
) {}
