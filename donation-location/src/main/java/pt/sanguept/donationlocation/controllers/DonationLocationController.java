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
import pt.sanguept.donationlocation.mappers.DonationLocationMapper;
import pt.sanguept.donationlocation.services.DonationLocationService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Endpoints for managing donation locations.")
public class DonationLocationController {

    private final DonationLocationService donationLocationService;

    @GetMapping
    public ResponseEntity<Page<DonationLocationDto>> search(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long administrativeDivisionId,
            @RequestParam(required = false) Boolean active) {
        DonationLocationFilter filter = new DonationLocationFilter(name, administrativeDivisionId, active);
        return ResponseEntity.ok(donationLocationService.search(filter, pageable).map(DonationLocationMapper::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonationLocationDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(DonationLocationMapper.toDto(donationLocationService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<DonationLocationDto> create(@RequestBody DonationLocationRequestDto dto) {
        return ResponseEntity.ok(DonationLocationMapper.toDto(donationLocationService.create(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DonationLocationDto> update(@PathVariable UUID id, @RequestBody DonationLocationRequestDto dto) {
        return ResponseEntity.ok(DonationLocationMapper.toDto(donationLocationService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DonationLocationDto> deactivate(@PathVariable UUID id) {
        donationLocationService.deactivate(id);
        return ResponseEntity.ok(DonationLocationMapper.toDto(donationLocationService.findById(id)));
    }
}
