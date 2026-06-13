package pt.sanguept.user.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.user.dtos.CreateUserRequest;
import pt.sanguept.user.dtos.UpdateUserRequest;
import pt.sanguept.user.dtos.UserDto;
import pt.sanguept.user.entities.Role;
import pt.sanguept.user.entities.User;
import pt.sanguept.user.mappers.UserMapper;
import pt.sanguept.user.repositories.UserRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(CreateUserRequest request) {
        return createUser(request, false);
    }

    public User createUser(CreateUserRequest request, boolean enabled) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.rawPassword()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .enabled(enabled)
                .build();

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        return UserMapper.toDto(findById(id));
    }

    public void assignRole(User user, Role role) {
        user.getRoles().add(role);
        userRepository.save(user);
    }

    public void removeRole(User user, Role role) {
        user.getRoles().remove(role);
        userRepository.save(user);
    }

    public User enableUser(UUID id) {
        User user = findById(id);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User changePassword(UUID id, String newPassword) {
        User user = findById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setAuthVersion(user.getAuthVersion() + 1);
        return userRepository.save(user);
    }

    public User disableUser(UUID id) {
        User user = findById(id);
        user.setEnabled(false);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserMapper::toDto);
    }

    public UserDto update(UUID id, UpdateUserRequest request) {
        User user = findById(id);
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        return UserMapper.toDto(userRepository.save(user));
    }

    public void softDelete(UUID id) {
        User user = findById(id);
        user.setEnabled(false);
        user.setAuthVersion(user.getAuthVersion() + 1);
        userRepository.save(user);
    }

}
