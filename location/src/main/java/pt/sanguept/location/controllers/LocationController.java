package pt.sanguept.location.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sanguept.location.dtos.LocationDto;
import pt.sanguept.location.dtos.LocationFilter;
import pt.sanguept.location.dtos.LocationRequestDto;
import pt.sanguept.location.mappers.LocationMapper;
import pt.sanguept.location.services.LocationService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Endpoints for managing donation locations.")
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public ResponseEntity<Page<LocationDto>> search(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long administrativeDivisionId,
            @RequestParam(required = false) Boolean active) {
        LocationFilter filter = new LocationFilter(name, administrativeDivisionId, active);
        return ResponseEntity.ok(locationService.search(filter, pageable).map(LocationMapper::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(LocationMapper.toDto(locationService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<LocationDto> create(@RequestBody LocationRequestDto dto) {
        return ResponseEntity.ok(LocationMapper.toDto(locationService.create(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationDto> update(@PathVariable UUID id, @RequestBody LocationRequestDto dto) {
        return ResponseEntity.ok(LocationMapper.toDto(locationService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<LocationDto> deactivate(@PathVariable UUID id) {
        locationService.deactivate(id);
        return ResponseEntity.ok(LocationMapper.toDto(locationService.findById(id)));
    }
}
