package pt.sanguept.donationlocation.services;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.donationlocation.dtos.LocationFilter;
import pt.sanguept.donationlocation.dtos.LocationRequestDto;
import pt.sanguept.donationlocation.entities.Location;
import pt.sanguept.donationlocation.repositories.LocationRepository;
import pt.sanguept.territory.entities.AdministrativeDivision;
import pt.sanguept.territory.repositories.AdministrativeDivisionRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final LocationRepository locationRepository;
    private final AdministrativeDivisionRepository administrativeDivisionRepository;

    public Page<Location> search(LocationFilter filter, Pageable pageable) {
        var spec = nameContains(filter.name())
                .and(administrativeDivisionIdEq(filter.administrativeDivisionId()))
                .and(activeEq(filter.active()));
        return locationRepository.findAll(spec, pageable);
    }

    public Location findById(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
    }

    @Transactional
    public Location create(LocationRequestDto dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (dto.address() == null || dto.address().isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (dto.latitude() == null) {
            throw new IllegalArgumentException("Latitude is required");
        }
        if (dto.longitude() == null) {
            throw new IllegalArgumentException("Longitude is required");
        }
        if (dto.administrativeDivisionId() == null) {
            throw new IllegalArgumentException("Administrative division is required");
        }

        AdministrativeDivision division = administrativeDivisionRepository.findById(dto.administrativeDivisionId())
                .orElseThrow(() -> new IllegalArgumentException("Administrative division not found: " + dto.administrativeDivisionId()));

        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(dto.longitude(), dto.latitude()));

        Location location = new Location();
        location.setName(dto.name());
        location.setAddress(dto.address());
        location.setCoordinates(point);
        location.setAdministrativeDivision(division);
        location.setActive(dto.active() != null ? dto.active() : true);
        location.setExternalId(dto.externalId());

        return locationRepository.save(location);
    }

    @Transactional
    public Location update(UUID id, LocationRequestDto dto) {
        Location location = findById(id);

        if (dto.name() != null && !dto.name().isBlank()) {
            location.setName(dto.name());
        }
        if (dto.address() != null && !dto.address().isBlank()) {
            location.setAddress(dto.address());
        }
        if (dto.latitude() != null && dto.longitude() != null) {
            Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(dto.longitude(), dto.latitude()));
            location.setCoordinates(point);
        }
        if (dto.administrativeDivisionId() != null) {
            AdministrativeDivision division = administrativeDivisionRepository.findById(dto.administrativeDivisionId())
                    .orElseThrow(() -> new IllegalArgumentException("Administrative division not found: " + dto.administrativeDivisionId()));
            location.setAdministrativeDivision(division);
        }
        if (dto.active() != null) {
            location.setActive(dto.active());
        }
        if (dto.externalId() != null) {
            location.setExternalId(dto.externalId());
        }

        return locationRepository.save(location);
    }

    @Transactional
    public void deactivate(UUID id) {
        Location location = findById(id);
        location.setActive(false);
        locationRepository.save(location);
    }

    static Specification<Location> nameContains(String query) {
        return (root, cq, cb) -> {
            if (query == null || query.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + query.toLowerCase() + "%");
        };
    }

    static Specification<Location> administrativeDivisionIdEq(Long divisionId) {
        return (root, cq, cb) ->
                divisionId == null ? cb.conjunction() : cb.equal(root.get("administrativeDivision").get("id"), divisionId);
    }

    static Specification<Location> activeEq(Boolean active) {
        return (root, cq, cb) ->
                active == null ? cb.conjunction() : cb.equal(root.get("active"), active);
    }
}
