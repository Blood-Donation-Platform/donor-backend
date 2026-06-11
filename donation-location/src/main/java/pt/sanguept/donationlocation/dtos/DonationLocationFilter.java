package pt.sanguept.donationlocation.dtos;

import java.util.UUID;

public record DonationLocationFilter(
        String name,
        UUID administrativeDivisionId,
        Boolean active
) {}
