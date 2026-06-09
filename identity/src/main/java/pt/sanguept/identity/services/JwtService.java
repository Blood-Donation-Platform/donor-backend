package pt.sanguept.identity.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import pt.sanguept.commoncore.models.AppPrincipal;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long accessTokenExpiryMinutes;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${jwt.access-token-expiry-minutes:15}") long accessTokenExpiryMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
    }

    public String generateToken(AppPrincipal principal) {
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenExpiryMinutes, ChronoUnit.MINUTES))
                .subject(principal.getId().toString())
                .claim("roles", roles)
                .claim("authVersion", principal.getAuthVersion())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiryMinutes * 60;
    }

}
