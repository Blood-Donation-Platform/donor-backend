package pt.sanguept.donationlocation.dtos;

public record DonationLocationFilter(
        String name,
        Long administrativeDivisionId,
        Boolean active
) {}
