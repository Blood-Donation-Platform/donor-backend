package pt.sanguept.auth.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.sanguept.auth.dtos.*;
import pt.sanguept.auth.services.AuthService;
import pt.sanguept.auth.services.JwtService;
import pt.sanguept.auth.services.VerificationTokenService;
import pt.sanguept.user.dtos.CreateUserRequest;
import pt.sanguept.user.services.RoleService;
import pt.sanguept.user.services.UserService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserService userService;
    private final RoleService roleService;
    private final VerificationTokenService tokenService;

    public AuthController(AuthService authService, JwtService jwtService,
                          UserService userService, RoleService roleService,
                          VerificationTokenService tokenService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userService = userService;
        this.roleService = roleService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody CreateUserRequest request) {
        var user = userService.createUser(request);
        roleService.assignRoleToUser(user.getId(), "ROLE_USER");
        tokenService.createAndSendVerificationEmail(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful. Check your email to verify your account."));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        tokenService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully. You can now log in."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        tokenService.createAndSendPasswordResetEmail(request.email());
        return ResponseEntity.ok(Map.of("message", "If an account exists with that email, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        tokenService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
    }

    @PostMapping("/resend-verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestParam UUID userId) {
        tokenService.sendVerificationEmailForUser(userId);
        return ResponseEntity.ok(Map.of("message", "Verification email sent."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        TokenPairResponse tokens = authService.login(request.email(), request.password());

        LoginResponse response = LoginResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .expiresInSeconds(jwtService.getAccessTokenExpirySeconds())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@RequestBody RefreshRequest request) {
        TokenPairResponse tokens = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok().build();
    }

}
