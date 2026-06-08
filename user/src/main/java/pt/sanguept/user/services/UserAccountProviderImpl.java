package pt.sanguept.user.services;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import pt.sanguept.commoncore.interfaces.UserAccountProvider;
import pt.sanguept.commoncore.models.AppPrincipal;
import pt.sanguept.user.entities.User;
import pt.sanguept.user.repositories.UserRepository;

import java.util.Collections;
import java.util.Optional;

@Component
public class UserAccountProviderImpl implements UserAccountProvider {

    private final UserRepository userRepository;

    public UserAccountProviderImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<AppPrincipal> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::toAppPrincipal);
    }

    private AppPrincipal toAppPrincipal(User user) {
        return AppPrincipal.builder()
                .id(user.getId())
                .identifier(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(Collections.<SimpleGrantedAuthority>emptyList())
                .enabled(user.isEnabled())
                .accountNonLocked(true)
                .build();
    }
}
