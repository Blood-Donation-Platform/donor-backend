package pt.sanguept.identity.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminDemoController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminDashboard() {
        return ResponseEntity.ok("Welcome to admin dashboard");
    }

    @GetMapping("/user-area")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> userArea() {
        return ResponseEntity.ok("Welcome to user area");
    }

}
