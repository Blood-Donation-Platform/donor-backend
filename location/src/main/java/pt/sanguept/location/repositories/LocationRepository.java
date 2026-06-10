package pt.sanguept.location.repositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pt.sanguept.commoninfra.repositories.BaseRepository;
import pt.sanguept.location.entities.Location;

import java.util.UUID;

@Repository
public interface LocationRepository extends BaseRepository<Location, UUID>, JpaSpecificationExecutor<Location> {
}
