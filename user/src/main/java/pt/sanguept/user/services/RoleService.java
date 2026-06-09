package pt.sanguept.user.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.user.entities.Role;
import pt.sanguept.user.entities.User;
import pt.sanguept.user.repositories.RoleRepository;
import pt.sanguept.user.repositories.UserRepository;

import java.util.Set;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public Role createRole(String name) {
        if (!name.startsWith("ROLE_")) {
            throw new IllegalArgumentException("Role name must start with ROLE_");
        }
        if (roleRepository.existsByName(name)) {
            throw new IllegalArgumentException("Role already exists: " + name);
        }
        Role role = Role.builder()
                .name(name)
                .build();
        return roleRepository.save(role);
    }

    public void assignRoleToUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        user.getRoles().add(role);
        userRepository.save(user);
    }

    public void removeRoleFromUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        user.getRoles().remove(role);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Set<Role> getRolesForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return user.getRoles();
    }

}
