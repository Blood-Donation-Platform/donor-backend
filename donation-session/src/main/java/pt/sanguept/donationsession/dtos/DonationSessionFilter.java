package pt.sanguept.donationsession.dtos;

import pt.sanguept.donationsession.enums.SessionStatus;

import java.time.LocalDate;
import java.util.UUID;

public record DonationSessionFilter(
        UUID locationId,
        SessionStatus status,
        LocalDate startsAfter,
        LocalDate endsBefore
) {}
