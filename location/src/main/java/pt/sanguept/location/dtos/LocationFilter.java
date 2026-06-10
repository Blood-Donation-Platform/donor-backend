package pt.sanguept.location.dtos;

public record LocationFilter(
        String name,
        Long administrativeDivisionId,
        Boolean active
) {}
