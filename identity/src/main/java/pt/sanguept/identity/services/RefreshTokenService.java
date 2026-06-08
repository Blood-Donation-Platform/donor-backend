package pt.sanguept.identity.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.identity.entities.RefreshToken;
import pt.sanguept.identity.repositories.RefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Transactional
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final int refreshTokenExpiryDays;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${jwt.refresh-token-expiry-days:7}") int refreshTokenExpiryDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }

    public String createToken(Long userId, String deviceId) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = hash(rawToken);

        RefreshToken entity = RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId(userId)
                .expiresAt(Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .deviceId(deviceId)
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    public TokenRotationResult validateAndRotate(String rawToken) {
        String tokenHash = hash(rawToken);
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (oldToken.isRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        if (oldToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        Long userId = oldToken.getUserId();

        oldToken.setRevoked(true);
        oldToken.setUsedAt(Instant.now());

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String newRawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String newTokenHash = hash(newRawToken);

        RefreshToken newToken = RefreshToken.builder()
                .tokenHash(newTokenHash)
                .userId(userId)
                .expiresAt(Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .deviceId(oldToken.getDeviceId())
                .build();

        refreshTokenRepository.save(newToken);
        oldToken.setReplacedByTokenId(newToken.getId());
        refreshTokenRepository.save(oldToken);

        return new TokenRotationResult(newRawToken, userId);
    }

    public void revoke(String rawToken) {
        String tokenHash = hash(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        token.setRevoked(true);
        token.setUsedAt(Instant.now());
        refreshTokenRepository.save(token);
    }

    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record TokenRotationResult(String newRawToken, Long userId) {}

}
