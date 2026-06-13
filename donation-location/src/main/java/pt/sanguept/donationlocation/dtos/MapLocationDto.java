package pt.sanguept.donationlocation.dtos;

import java.util.UUID;

public record MapLocationDto(UUID id, String name, double latitude, double longitude) {}
