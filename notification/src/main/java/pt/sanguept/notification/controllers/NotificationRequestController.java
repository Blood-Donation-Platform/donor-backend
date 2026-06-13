package pt.sanguept.notification.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.sanguept.notification.dtos.NotificationRequestDto;
import pt.sanguept.notification.services.NotificationRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notification-requests")
@RequiredArgsConstructor
@Tag(name = "Notification Requests", description = "Endpoints for inspecting notification requests.")
public class NotificationRequestController {

    private final NotificationRequestService service;

    @GetMapping("/failed")
    public ResponseEntity<List<NotificationRequestDto>> listFailed() {
        return ResponseEntity.ok(service.findFailed());
    }

}
