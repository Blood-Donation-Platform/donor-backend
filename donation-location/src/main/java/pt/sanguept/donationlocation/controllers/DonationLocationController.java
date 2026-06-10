package pt.sanguept.donationlocation.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sanguept.donationlocation.dtos.DonationLocationDto;
import pt.sanguept.donationlocation.dtos.DonationLocationFilter;
import pt.sanguept.donationlocation.dtos.DonationLocationRequestDto;
import pt.sanguept.donationlocation.mappers.LocationMapper;
import pt.sanguept.donationlocation.services.LocationService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Endpoints for managing donation locations.")
public class DonationLocationController {

    private final LocationService locationService;

    @GetMapping
    public ResponseEntity<Page<DonationLocationDto>> search(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long administrativeDivisionId,
            @RequestParam(required = false) Boolean active) {
        DonationLocationFilter filter = new DonationLocationFilter(name, administrativeDivisionId, active);
        return ResponseEntity.ok(locationService.search(filter, pageable).map(LocationMapper::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonationLocationDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(LocationMapper.toDto(locationService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<DonationLocationDto> create(@RequestBody DonationLocationRequestDto dto) {
        return ResponseEntity.ok(LocationMapper.toDto(locationService.create(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DonationLocationDto> update(@PathVariable UUID id, @RequestBody DonationLocationRequestDto dto) {
        return ResponseEntity.ok(LocationMapper.toDto(locationService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DonationLocationDto> deactivate(@PathVariable UUID id) {
        locationService.deactivate(id);
        return ResponseEntity.ok(LocationMapper.toDto(locationService.findById(id)));
    }
}
