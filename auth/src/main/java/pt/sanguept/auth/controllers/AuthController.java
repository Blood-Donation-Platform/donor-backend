package pt.sanguept.auth.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.sanguept.auth.dtos.LoginRequest;
import pt.sanguept.auth.dtos.LoginResponse;
import pt.sanguept.auth.dtos.RefreshRequest;
import pt.sanguept.auth.dtos.TokenPairResponse;
import pt.sanguept.auth.services.AuthService;
import pt.sanguept.auth.services.JwtService;
import pt.sanguept.user.dtos.CreateUserRequest;
import pt.sanguept.user.dtos.UserDto;
import pt.sanguept.user.mappers.UserMapper;
import pt.sanguept.user.services.RoleService;
import pt.sanguept.user.services.UserService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserService userService;
    private final RoleService roleService;

    public AuthController(AuthService authService, JwtService jwtService,
                          UserService userService, RoleService roleService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userService = userService;
        this.roleService = roleService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody CreateUserRequest request) {
        var user = userService.createUser(request);
        roleService.assignRoleToUser(user.getId(), "ROLE_USER");
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toDto(user));
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
