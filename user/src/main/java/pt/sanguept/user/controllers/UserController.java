package pt.sanguept.user.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.sanguept.user.dtos.UpdateUserRequest;
import pt.sanguept.user.dtos.UserDto;
import pt.sanguept.user.services.RoleService;
import pt.sanguept.user.services.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Management", description = "Endpoints for managing users (admin only).")
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<Page<UserDto>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.list(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserDto> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/roles/{roleName}")
    public ResponseEntity<Void> assignRole(@PathVariable UUID userId, @PathVariable String roleName) {
        roleService.assignRoleToUser(userId, roleName);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    public ResponseEntity<Void> removeRole(@PathVariable UUID userId, @PathVariable String roleName) {
        roleService.removeRoleFromUser(userId, roleName);
        return ResponseEntity.noContent().build();
    }
}
