package pt.sanguept.donationsession.dtos;

import pt.sanguept.donationlocation.dtos.DonationLocationDto;

import java.util.List;

public record LocationDetailDto(DonationLocationDto location, List<DonationSessionDto> upcomingSessions) {}
