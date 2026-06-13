package pt.sanguept.user.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.sanguept.user.dtos.ChangePasswordRequest;
import pt.sanguept.user.dtos.CreateUserRequest;
import pt.sanguept.user.dtos.UpdateProfileRequest;
import pt.sanguept.user.dtos.UpdateUserRequest;
import pt.sanguept.user.dtos.UserDto;
import pt.sanguept.user.entities.Role;
import pt.sanguept.user.entities.User;
import pt.sanguept.user.repositories.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(any())).thenReturn("encoded-pass");
    }

    @Test
    void shouldCreateUserDisabledByDefault() {
        var request = createRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User user = userService.createUser(request);

        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getEmail()).isEqualTo(request.email());
        assertThat(user.getPasswordHash()).isEqualTo("encoded-pass");
    }

    @Test
    void shouldCreateUserEnabledWhenSpecified() {
        var request = createRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User user = userService.createUser(request, true);

        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void shouldRejectDuplicateEmail() {
        var request = createRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void shouldFindById() {
        var user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThat(userService.findById(USER_ID)).isEqualTo(user);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldReturnUserDtoWithRoles() {
        var user = existingUser();
        user.setRoles(Set.of(role("ROLE_USER"), role("ROLE_ADMIN")));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserDto dto = userService.getUserById(USER_ID);

        assertThat(dto.id()).isEqualTo(USER_ID);
        assertThat(dto.roles()).containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void shouldUpdateUserFields() {
        var user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        var request = new UpdateUserRequest("NewFirst", "NewLast", false);
        UserDto dto = userService.update(USER_ID, request);

        assertThat(dto.firstName()).isEqualTo("NewFirst");
        assertThat(dto.lastName()).isEqualTo("NewLast");
        assertThat(dto.enabled()).isFalse();
    }

    @Test
    void shouldOnlyUpdateNonNullFields() {
        var user = existingUser();
        user.setFirstName("Original");
        user.setEnabled(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        var request = new UpdateUserRequest(null, "NewLast", null);
        UserDto dto = userService.update(USER_ID, request);

        assertThat(dto.firstName()).isEqualTo("Original");
        assertThat(dto.lastName()).isEqualTo("NewLast");
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    void shouldUpdateProfile() {
        var user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserDto dto = userService.updateProfile(USER_ID, new UpdateProfileRequest("Ana", "Silva"));

        assertThat(dto.firstName()).isEqualTo("Ana");
        assertThat(dto.lastName()).isEqualTo("Silva");
    }

    @Test
    void shouldChangePasswordWithVerification() {
        var user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("new-hash");

        userService.changePasswordWithVerification(USER_ID,
                new ChangePasswordRequest("oldPass", "newPass"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getAuthVersion()).isEqualTo(2);
    }

    @Test
    void shouldRejectWrongCurrentPassword() {
        var user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePasswordWithVerification(USER_ID,
                new ChangePasswordRequest("wrongPass", "newPass")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void shouldRejectSamePassword() {
        var user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("samePass", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePasswordWithVerification(USER_ID,
                new ChangePasswordRequest("samePass", "samePass")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");
    }

    @Test
    void shouldSoftDeleteUser() {
        var user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.softDelete(USER_ID);

        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getAuthVersion()).isEqualTo(2);
        verify(userRepository).save(user);
    }

    @Test
    void shouldListUsersPaginated() {
        var user = existingUser();
        user.setRoles(Set.of(role("ROLE_USER")));
        when(userRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<UserDto> page = userService.list(PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).email()).isEqualTo("test@example.com");
        assertThat(page.getContent().get(0).roles()).contains("ROLE_USER");
    }

    @Test
    void shouldEnableUser() {
        var user = existingUser();
        user.setEnabled(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.enableUser(USER_ID);

        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void shouldChangePasswordDirectly() {
        var user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass")).thenReturn("new-hash");

        userService.changePassword(USER_ID, "newPass");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getAuthVersion()).isEqualTo(2);
    }

    @Test
    void shouldAssignRole() {
        var user = existingUser();
        var role = role("ROLE_MODERATOR");
        user.setRoles(new java.util.HashSet<>());

        userService.assignRole(user, role);

        assertThat(user.getRoles()).contains(role);
        verify(userRepository).save(user);
    }

    @Test
    void shouldRemoveRole() {
        var user = existingUser();
        var role = role("ROLE_USER");
        user.setRoles(new java.util.HashSet<>(Set.of(role)));

        userService.removeRole(user, role);

        assertThat(user.getRoles()).doesNotContain(role);
        verify(userRepository).save(user);
    }

    private CreateUserRequest createRequest() {
        return CreateUserRequest.builder()
                .email("test@example.com").rawPassword("pass").firstName("Test").lastName("User").build();
    }

    private User existingUser() {
        return User.builder()
                .id(USER_ID).email("test@example.com").passwordHash("old-hash")
                .firstName("Test").lastName("User").enabled(true).authVersion(1)
                .build();
    }

    private Role role(String name) {
        return Role.builder().id(UUID.randomUUID()).name(name).build();
    }
}
