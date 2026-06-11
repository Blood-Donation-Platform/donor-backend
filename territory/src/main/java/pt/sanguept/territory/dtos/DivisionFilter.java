package pt.sanguept.territory.dtos;

import java.util.UUID;

public record DivisionFilter(
        String name,
        UUID parentId,
        Boolean rootOnly
) {}
