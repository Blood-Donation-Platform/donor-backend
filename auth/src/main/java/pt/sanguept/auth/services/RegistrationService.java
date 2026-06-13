package pt.sanguept.auth.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.auth.entities.VerificationToken;
import pt.sanguept.auth.enums.TokenType;
import pt.sanguept.communications.email.EmailService;
import pt.sanguept.user.dtos.CreateUserRequest;
import pt.sanguept.user.entities.User;
import pt.sanguept.user.services.RoleService;
import pt.sanguept.user.services.UserService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationService {

    private final UserService userService;
    private final RoleService roleService;
    private final VerificationTokenService tokenService;
    private final EmailService emailService;

    public User register(CreateUserRequest request) {
        User user = userService.createUser(request);
        roleService.assignRoleToUser(user.getId(), "ROLE_USER");
        VerificationToken token = tokenService.createToken(user.getId(), TokenType.EMAIL_VERIFICATION,
                Instant.now().plus(24, ChronoUnit.HOURS));
        emailService.sendTemplate(user.getEmail(), "verify-email", Map.of(
                "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                "token", token.getToken()
        ));
        return user;
    }
}
