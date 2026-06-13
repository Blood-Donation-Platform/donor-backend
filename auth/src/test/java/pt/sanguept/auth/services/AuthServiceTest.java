package pt.sanguept.auth.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import pt.sanguept.auth.dtos.TokenPairResponse;
import pt.sanguept.commoncore.interfaces.UserAccountProvider;
import pt.sanguept.commoncore.models.AppPrincipal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserAccountProvider userAccountProvider;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private static final UUID USER_ID = UUID.randomUUID();
    private AppPrincipal enabledPrincipal;
    private AppPrincipal disabledPrincipal;

    @BeforeEach
    void setUp() {
        enabledPrincipal = AppPrincipal.builder()
                .id(USER_ID).identifier("user@test.com")
                .enabled(true).accountNonLocked(true)
                .authVersion(1).authorities(List.of()).build();

        disabledPrincipal = AppPrincipal.builder()
                .id(USER_ID).identifier("user@test.com")
                .enabled(false).accountNonLocked(true)
                .authVersion(1).authorities(List.of()).build();
    }

    @Test
    void shouldLoginAndReturnTokens() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(enabledPrincipal);
        when(jwtService.generateToken(enabledPrincipal)).thenReturn("access-token");
        when(refreshTokenService.createToken(USER_ID, null)).thenReturn("refresh-token");

        TokenPairResponse result = authService.login("user@test.com", "password");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void shouldRejectLoginForDisabledUser() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(disabledPrincipal);

        assertThatThrownBy(() -> authService.login("user@test.com", "password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void shouldRefreshToken() {
        var rotation = new RefreshTokenService.TokenRotationResult("new-refresh", USER_ID);
        when(refreshTokenService.validateAndRotate("old-refresh")).thenReturn(rotation);
        when(userAccountProvider.findById(USER_ID)).thenReturn(Optional.of(enabledPrincipal));
        when(jwtService.generateToken(enabledPrincipal)).thenReturn("new-access");

        TokenPairResponse result = authService.refresh("old-refresh");

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void shouldRejectRefreshForDisabledUser() {
        var rotation = new RefreshTokenService.TokenRotationResult("new-refresh", USER_ID);
        when(refreshTokenService.validateAndRotate("old-refresh")).thenReturn(rotation);
        when(userAccountProvider.findById(USER_ID)).thenReturn(Optional.of(disabledPrincipal));

        assertThatThrownBy(() -> authService.refresh("old-refresh"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void shouldRejectRefreshForDeletedUser() {
        var rotation = new RefreshTokenService.TokenRotationResult("new-refresh", USER_ID);
        when(refreshTokenService.validateAndRotate("old-refresh")).thenReturn(rotation);
        when(userAccountProvider.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("old-refresh"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldRevokeTokenOnLogout() {
        authService.logout("refresh-token");

        verify(refreshTokenService).revoke("refresh-token");
    }
}
