package pt.sanguept.identity.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pt.sanguept.commoncore.interfaces.UserAccountProvider;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserAccountProvider userAccountProvider;

    public UserDetailsServiceImpl(UserAccountProvider userAccountProvider) {
        this.userAccountProvider = userAccountProvider;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userAccountProvider.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

}
