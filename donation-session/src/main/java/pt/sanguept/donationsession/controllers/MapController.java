package pt.sanguept.donationsession.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sanguept.donationlocation.dtos.MapBounds;
import pt.sanguept.donationlocation.dtos.MapLocationDto;
import pt.sanguept.donationsession.dtos.LocationDetailDto;
import pt.sanguept.donationsession.services.DonationSessionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
@Tag(name = "Map", description = "Endpoints for map-based location and session browsing.")
public class MapController {

    private final DonationSessionService service;

    @GetMapping("/locations")
    public ResponseEntity<List<MapLocationDto>> getMapLocations(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng) {
        return ResponseEntity.ok(service.getMapLocations(new MapBounds(swLat, swLng, neLat, neLng)));
    }

    @GetMapping("/locations/{id}")
    public ResponseEntity<LocationDetailDto> getLocationDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getLocationDetail(id));
    }
}
