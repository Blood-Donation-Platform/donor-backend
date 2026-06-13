package pt.sanguept.user.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sanguept.commoncore.utils.SecurityUtils;
import pt.sanguept.user.dtos.ChangePasswordRequest;
import pt.sanguept.user.dtos.UpdateProfileRequest;
import pt.sanguept.user.dtos.UserDto;
import pt.sanguept.user.services.UserService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints for the authenticated user's own profile.")
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserDto> getProfile() {
        UUID userId = getUserId();
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PatchMapping
    public ResponseEntity<UserDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = getUserId();
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = getUserId();
        userService.changePasswordWithVerification(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully. Please log in again."));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteAccount() {
        UUID userId = getUserId();
        userService.softDelete(userId);
        return ResponseEntity.ok(Map.of("message", "Account deleted."));
    }

    private static UUID getUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("Not authenticated"));
    }
}
