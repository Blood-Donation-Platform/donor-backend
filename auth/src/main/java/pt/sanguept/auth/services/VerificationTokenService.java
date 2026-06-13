package pt.sanguept.auth.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.auth.entities.VerificationToken;
import pt.sanguept.auth.enums.TokenType;
import pt.sanguept.auth.repositories.VerificationTokenRepository;
import pt.sanguept.communications.email.EmailService;
import pt.sanguept.user.entities.User;
import pt.sanguept.user.services.UserService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;
    private final UserService userService;
    private final EmailService emailService;

    public VerificationToken createAndSendVerificationEmail(User user) {
        VerificationToken token = createToken(user.getId(), TokenType.EMAIL_VERIFICATION);
        emailService.sendTemplate(user.getEmail(), "verify-email", Map.of(
                "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                "token", token.getToken()
        ));
        return token;
    }

    public void createAndSendPasswordResetEmail(String email) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));
        VerificationToken token = createToken(user.getId(), TokenType.PASSWORD_RESET);
        emailService.sendTemplate(email, "reset-password", Map.of(
                "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                "token", token.getToken()
        ));
    }

    public User verifyEmail(String tokenValue) {
        VerificationToken token = validateToken(tokenValue, TokenType.EMAIL_VERIFICATION);
        token.setUsed(true);
        tokenRepository.save(token);
        userService.enableUser(token.getUserId());
        return userService.findById(token.getUserId());
    }

    public void resetPassword(String tokenValue, String newPassword) {
        VerificationToken token = validateToken(tokenValue, TokenType.PASSWORD_RESET);
        token.setUsed(true);
        tokenRepository.save(token);
        userService.changePassword(token.getUserId(), newPassword);
    }

    public void sendVerificationEmailForUser(UUID userId) {
        User user = userService.findById(userId);
        createAndSendVerificationEmail(user);
    }

    private VerificationToken createToken(UUID userId, TokenType type) {
        VerificationToken token = VerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .type(type)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        return tokenRepository.save(token);
    }

    private VerificationToken validateToken(String tokenValue, TokenType expectedType) {
        VerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (token.isUsed()) {
            throw new IllegalArgumentException("Token already used");
        }
        if (token.getType() != expectedType) {
            throw new IllegalArgumentException("Token type mismatch");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired");
        }
        return token;
    }
}
