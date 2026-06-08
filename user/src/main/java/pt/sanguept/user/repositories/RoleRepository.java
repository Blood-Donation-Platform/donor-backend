package pt.sanguept.user.repositories;

import org.springframework.stereotype.Repository;
import pt.sanguept.commoninfra.repositories.BaseRepository;
import pt.sanguept.user.entities.Role;

import java.util.Optional;

@Repository
public interface RoleRepository extends BaseRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

}
