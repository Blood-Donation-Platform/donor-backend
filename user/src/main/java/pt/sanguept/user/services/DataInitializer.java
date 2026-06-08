package pt.sanguept.user.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import pt.sanguept.user.dtos.CreateUserRequest;
import pt.sanguept.user.repositories.RoleRepository;
import pt.sanguept.user.repositories.UserRepository;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final RoleService roleService;
    private final UserRepository userRepository;
    private final UserService userService;

    public DataInitializer(RoleRepository roleRepository, RoleService roleService,
                           UserRepository userRepository, UserService userService) {
        this.roleRepository = roleRepository;
        this.roleService = roleService;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedRoles();
        seedAdminUser();
    }

    private void seedRoles() {
        if (roleRepository.count() == 0) {
            roleService.createRole("ROLE_USER");
            roleService.createRole("ROLE_ADMIN");
            log.info("Seeded roles: ROLE_USER, ROLE_ADMIN");
        }
    }

    private void seedAdminUser() {
        if (userRepository.count() == 0) {
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("admin@sanguept.pt")
                    .rawPassword("admin")
                    .firstName("Admin")
                    .lastName("User")
                    .build();
            userService.createUser(request);
            roleService.assignRoleToUser(
                    userService.findByEmail("admin@sanguept.pt").orElseThrow().getId(),
                    "ROLE_ADMIN"
            );
            log.info("Seeded admin user: admin@sanguept.pt / admin");
        }
    }

}
