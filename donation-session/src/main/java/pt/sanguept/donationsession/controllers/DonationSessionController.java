package pt.sanguept.donationsession.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sanguept.donationsession.dtos.DonationSessionDto;
import pt.sanguept.donationsession.dtos.DonationSessionFilter;
import pt.sanguept.donationsession.dtos.DonationSessionRequestDto;
import pt.sanguept.donationsession.enums.SessionStatus;
import pt.sanguept.donationsession.mappers.DonationSessionMapper;
import pt.sanguept.donationsession.services.DonationSessionService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "Donation Sessions", description = "Endpoints for managing donation sessions.")
public class DonationSessionController {

    private final DonationSessionService service;

    @PostMapping
    public ResponseEntity<DonationSessionDto> create(@Valid @RequestBody DonationSessionRequestDto dto) {
        var session = service.createSession(dto);
        return ResponseEntity.ok(DonationSessionMapper.toDto(session));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonationSessionDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(DonationSessionMapper.toDto(service.getById(id)));
    }

    @GetMapping
    public ResponseEntity<Page<DonationSessionDto>> search(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) SessionStatus status,
            @RequestParam(required = false) LocalDate startsAfter,
            @RequestParam(required = false) LocalDate endsBefore) {
        var filter = new DonationSessionFilter(locationId, status, startsAfter, endsBefore);
        var page = service.search(filter, pageable);
        return ResponseEntity.ok(DonationSessionMapper.toDto(page));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DonationSessionDto> update(@PathVariable UUID id,
                                                      @RequestBody DonationSessionRequestDto dto) {
        return ResponseEntity.ok(DonationSessionMapper.toDto(service.updateSession(id, dto)));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<DonationSessionDto> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(DonationSessionMapper.toDto(service.publishSession(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<DonationSessionDto> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(DonationSessionMapper.toDto(service.cancelSession(id)));
    }
}
