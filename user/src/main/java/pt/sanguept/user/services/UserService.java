package pt.sanguept.user.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.user.dtos.CreateUserRequest;
import pt.sanguept.user.entities.Role;
import pt.sanguept.user.entities.User;
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
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.rawPassword()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .enabled(true)
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

    public User disableUser(UUID id) {
        User user = findById(id);
        user.setEnabled(false);
        return userRepository.save(user);
    }

}
