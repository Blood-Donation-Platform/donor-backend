package pt.sanguept.donationlocation.mappers;

import pt.sanguept.donationlocation.dtos.DonationLocationDto;
import pt.sanguept.donationlocation.entities.Location;

import java.util.List;

public class LocationMapper {

    private LocationMapper() {}

    public static DonationLocationDto toDto(Location entity) {
        if (entity == null) return null;
        return new DonationLocationDto(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                entity.getCoordinates() != null ? entity.getCoordinates().getY() : null,
                entity.getCoordinates() != null ? entity.getCoordinates().getX() : null,
                entity.getAdministrativeDivision() != null ? entity.getAdministrativeDivision().getId() : null,
                entity.getAdministrativeDivision() != null ? entity.getAdministrativeDivision().getName() : null,
                entity.getActive(),
                entity.getExternalId()
        );
    }

    public static List<DonationLocationDto> toDtoList(List<Location> entities) {
        if (entities == null) return List.of();
        return entities.stream().map(LocationMapper::toDto).toList();
    }
}
