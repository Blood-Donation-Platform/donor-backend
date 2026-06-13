package pt.sanguept.auth.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.sanguept.auth.entities.VerificationToken;
import pt.sanguept.auth.enums.TokenType;
import pt.sanguept.communications.email.EmailService;
import pt.sanguept.user.dtos.CreateUserRequest;
import pt.sanguept.user.entities.User;
import pt.sanguept.user.services.RoleService;
import pt.sanguept.user.services.UserService;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private VerificationTokenService tokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void shouldCreateUserAssignRoleCreateTokenAndSendEmail() {
        var request = CreateUserRequest.builder()
                .email("test@example.com").rawPassword("pass").firstName("Test").build();
        var user = User.builder().id(UUID.randomUUID()).email(request.email())
                .firstName(request.firstName()).build();
        var token = VerificationToken.builder()
                .token("abc-123").userId(user.getId()).type(TokenType.EMAIL_VERIFICATION).build();

        when(userService.createUser(request)).thenReturn(user);
        when(tokenService.createToken(eq(user.getId()), eq(TokenType.EMAIL_VERIFICATION), any()))
                .thenReturn(token);

        User result = registrationService.register(request);

        assertThat(result).isEqualTo(user);
        verify(userService).createUser(request);
        verify(roleService).assignRoleToUser(user.getId(), "ROLE_USER");
        verify(tokenService).createToken(eq(user.getId()), eq(TokenType.EMAIL_VERIFICATION), any());
        verify(emailService).sendTemplate(eq(user.getEmail()), eq("verify-email"), any());
    }

    @Test
    void shouldPropagateEmailFailure() {
        var request = CreateUserRequest.builder()
                .email("test@example.com").rawPassword("pass").build();
        var user = User.builder().id(UUID.randomUUID()).email(request.email()).build();

        when(userService.createUser(request)).thenReturn(user);
        when(tokenService.createToken(any(), any(), any()))
                .thenReturn(VerificationToken.builder().token("tok").build());
        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .sendTemplate(any(), any(), any());

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("SMTP down");
    }

    @Test
    void shouldPassFirstNameAndTokenToEmailTemplate() {
        var request = CreateUserRequest.builder()
                .email("test@example.com").rawPassword("pass").firstName("João").build();
        var user = User.builder().id(UUID.randomUUID()).email(request.email())
                .firstName("João").build();
        var token = VerificationToken.builder().token("token-xyz").build();

        when(userService.createUser(request)).thenReturn(user);
        when(tokenService.createToken(any(), any(), any())).thenReturn(token);

        registrationService.register(request);

        verify(emailService).sendTemplate(eq(user.getEmail()), eq("verify-email"),
                eq(Map.of("firstName", "João", "token", "token-xyz")));
    }

    @Test
    void shouldPassEmptyFirstNameWhenNull() {
        var request = CreateUserRequest.builder()
                .email("test@example.com").rawPassword("pass").firstName(null).build();
        var user = User.builder().id(UUID.randomUUID()).email(request.email())
                .firstName(null).build();
        var token = VerificationToken.builder().token("token-xyz").build();

        when(userService.createUser(request)).thenReturn(user);
        when(tokenService.createToken(any(), any(), any())).thenReturn(token);

        registrationService.register(request);

        verify(emailService).sendTemplate(eq(user.getEmail()), eq("verify-email"),
                eq(Map.of("firstName", "", "token", "token-xyz")));
    }
}
