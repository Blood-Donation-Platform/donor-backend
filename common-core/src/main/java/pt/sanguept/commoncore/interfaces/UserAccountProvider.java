package pt.sanguept.commoncore.interfaces;

import pt.sanguept.commoncore.models.AppPrincipal;

import java.util.Optional;

public interface UserAccountProvider {
    Optional<AppPrincipal> findByEmail(String email);
}
