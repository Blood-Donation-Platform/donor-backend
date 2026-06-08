package pt.sanguept.identity.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.commoncore.interfaces.UserAccountProvider;
import pt.sanguept.commoncore.models.AppPrincipal;
import pt.sanguept.identity.dtos.TokenPairResponse;

@Service
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserAccountProvider userAccountProvider;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       UserAccountProvider userAccountProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userAccountProvider = userAccountProvider;
    }

    public TokenPairResponse login(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        AppPrincipal principal = (AppPrincipal) authentication.getPrincipal();

        if (!principal.isEnabled()) {
            throw new IllegalArgumentException("Account is disabled");
        }

        String accessToken = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.createToken(principal.getId(), null);

        return new TokenPairResponse(accessToken, refreshToken);
    }

    public TokenPairResponse refresh(String rawRefreshToken) {
        RefreshTokenService.TokenRotationResult rotation =
                refreshTokenService.validateAndRotate(rawRefreshToken);

        AppPrincipal principal = userAccountProvider.findById(rotation.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + rotation.userId()));

        if (!principal.isEnabled()) {
            throw new IllegalArgumentException("Account is disabled");
        }

        String accessToken = jwtService.generateToken(principal);

        return new TokenPairResponse(accessToken, rotation.newRawToken());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

}
