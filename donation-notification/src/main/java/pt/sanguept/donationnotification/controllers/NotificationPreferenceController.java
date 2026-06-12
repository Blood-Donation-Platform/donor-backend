package pt.sanguept.donationnotification.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sanguept.commoncore.utils.SecurityUtils;
import pt.sanguept.donationnotification.dtos.NotificationPreferenceDto;
import pt.sanguept.donationnotification.services.NotificationPreferenceService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification-preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "Endpoints for managing notification preferences.")
public class NotificationPreferenceController {

    private final NotificationPreferenceService service;

    @GetMapping
    public ResponseEntity<NotificationPreferenceDto> get() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("User not authenticated"));
        return ResponseEntity.ok(service.get(userId));
    }

    @PutMapping
    public ResponseEntity<NotificationPreferenceDto> update(@Valid @RequestBody NotificationPreferenceDto dto) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("User not authenticated"));
        return ResponseEntity.ok(service.update(userId, dto));
    }
}
