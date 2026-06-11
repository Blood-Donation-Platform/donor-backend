package pt.sanguept.commoncore.interfaces;

import pt.sanguept.commoncore.models.AppPrincipal;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountProvider {
    Optional<AppPrincipal> findByEmail(String email);

    Optional<AppPrincipal> findById(UUID id);
}
