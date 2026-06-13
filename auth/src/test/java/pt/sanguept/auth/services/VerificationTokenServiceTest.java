package pt.sanguept.auth.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.sanguept.auth.entities.VerificationToken;
import pt.sanguept.auth.enums.TokenType;
import pt.sanguept.auth.repositories.VerificationTokenRepository;
import pt.sanguept.communications.email.EmailService;
import pt.sanguept.user.entities.User;
import pt.sanguept.user.services.UserService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceTest {

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VerificationTokenService tokenService;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void shouldCreateTokenAndPersist() {
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var token = tokenService.createToken(USER_ID, TokenType.EMAIL_VERIFICATION,
                Instant.now().plus(24, ChronoUnit.HOURS));

        assertThat(token.getUserId()).isEqualTo(USER_ID);
        assertThat(token.getType()).isEqualTo(TokenType.EMAIL_VERIFICATION);
        assertThat(token.isUsed()).isFalse();
        assertThat(token.getToken()).isNotBlank();
        verify(tokenRepository).save(token);
    }

    @Test
    void shouldValidateAndVerifyEmail() {
        var token = validToken(USER_ID, TokenType.EMAIL_VERIFICATION);
        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        tokenService.verifyEmail(token.getToken());

        assertThat(token.isUsed()).isTrue();
        verify(tokenRepository).save(token);
        verify(userService).enableUser(USER_ID);
    }

    @Test
    void shouldThrowOnExpiredToken() {
        var token = expiredToken(USER_ID, TokenType.EMAIL_VERIFICATION);
        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tokenService.verifyEmail(token.getToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void shouldThrowOnAlreadyUsedToken() {
        var token = usedToken(USER_ID, TokenType.EMAIL_VERIFICATION);
        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tokenService.verifyEmail(token.getToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void shouldThrowOnTokenTypeMismatch() {
        var token = validToken(USER_ID, TokenType.PASSWORD_RESET);
        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tokenService.verifyEmail(token.getToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type mismatch");
    }

    @Test
    void shouldThrowOnInvalidToken() {
        when(tokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.verifyEmail("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    void shouldResetPassword() {
        var token = validToken(USER_ID, TokenType.PASSWORD_RESET);
        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        tokenService.resetPassword(token.getToken(), "newPassword");

        assertThat(token.isUsed()).isTrue();
        verify(tokenRepository).save(token);
        verify(userService).changePassword(USER_ID, "newPassword");
    }

    @Test
    void shouldSendPasswordResetEmailWhenUserFound() {
        var user = User.builder().id(USER_ID).email("u@test.com").firstName("João").build();
        var token = validToken(USER_ID, TokenType.PASSWORD_RESET);
        when(userService.findByEmail("u@test.com")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any())).thenReturn(token);

        tokenService.createAndSendPasswordResetEmail("u@test.com");

        verify(emailService).sendTemplate(eq("u@test.com"), eq("reset-password"),
                eq(Map.of("firstName", "João", "token", token.getToken())));
    }

    @Test
    void shouldNotSendPasswordResetWhenUserNotFound() {
        when(userService.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        tokenService.createAndSendPasswordResetEmail("unknown@test.com");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendTemplate(any(), any(), any());
    }

    @Test
    void shouldSendVerificationEmailForUser() {
        var user = User.builder().id(USER_ID).email("u@test.com").firstName("Ana").build();
        var token = validToken(USER_ID, TokenType.EMAIL_VERIFICATION);
        when(userService.findById(USER_ID)).thenReturn(user);
        when(tokenRepository.save(any())).thenReturn(token);

        tokenService.sendVerificationEmailForUser(USER_ID);

        verify(emailService).sendTemplate(eq("u@test.com"), eq("verify-email"),
                eq(Map.of("firstName", "Ana", "token", token.getToken())));
    }

    private VerificationToken validToken(UUID userId, TokenType type) {
        return VerificationToken.builder()
                .token("valid-token-" + type)
                .userId(userId)
                .type(type)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(false)
                .build();
    }

    private VerificationToken expiredToken(UUID userId, TokenType type) {
        return VerificationToken.builder()
                .token("expired-token-" + type)
                .userId(userId)
                .type(type)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .used(false)
                .build();
    }

    private VerificationToken usedToken(UUID userId, TokenType type) {
        return VerificationToken.builder()
                .token("used-token-" + type)
                .userId(userId)
                .type(type)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(true)
                .build();
    }
}
