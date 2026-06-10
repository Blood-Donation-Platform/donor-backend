package pt.sanguept.donationlocation.dtos;

public record LocationFilter(
        String name,
        Long administrativeDivisionId,
        Boolean active
) {}
