package pt.sanguept.commoncore.models;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pt.sanguept.commoncore.interfaces.AuthenticatedPrincipal;

import java.util.Collection;
import java.util.UUID;

@Builder
public class AppPrincipal implements UserDetails, AuthenticatedPrincipal {

    private final UUID id;
    @Getter
    private final String identifier; // username OR service name
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final int authVersion;

    @Override
    public UUID getId() {
        return id;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override public String getPassword() {
        return password;
    }

    @Override public String getUsername() {
        return identifier;
    }

    @Override public boolean isAccountNonExpired() {
        return true;
    }

    @Override public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override public boolean isEnabled() {
        return enabled;
    }

    @Override public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public int getAuthVersion() {
        return authVersion;
    }
}
