package pt.sanguept.notification.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sanguept.commoncore.utils.SecurityUtils;
import pt.sanguept.notification.dtos.NotificationSubscriptionDto;
import pt.sanguept.notification.dtos.NotificationSubscriptionRequestDto;
import pt.sanguept.notification.services.NotificationSubscriptionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification-subscriptions")
@RequiredArgsConstructor
@Tag(name = "Notification Subscriptions", description = "Endpoints for managing notification subscriptions.")
public class NotificationSubscriptionController {

    private final NotificationSubscriptionService service;

    @GetMapping
    public ResponseEntity<List<NotificationSubscriptionDto>> list() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("User not authenticated"));
        return ResponseEntity.ok(service.list(userId));
    }

    @PostMapping
    public ResponseEntity<NotificationSubscriptionDto> create(@Valid @RequestBody NotificationSubscriptionRequestDto dto) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("User not authenticated"));
        return ResponseEntity.ok(service.create(userId, dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<NotificationSubscriptionDto> update(@PathVariable UUID id,
                                                               @Valid @RequestBody NotificationSubscriptionRequestDto dto) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("User not authenticated"));
        return ResponseEntity.ok(service.update(id, userId, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("User not authenticated"));
        service.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
