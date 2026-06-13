package pt.sanguept.donationsession.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.sanguept.commoninfra.repositories.BaseRepository;
import pt.sanguept.donationsession.entities.DonationSession;

import java.util.List;
import java.util.UUID;

@Repository
public interface DonationSessionRepository extends BaseRepository<DonationSession, UUID>, JpaSpecificationExecutor<DonationSession> {

    @Query("SELECT s FROM DonationSession s WHERE s.location.id = :locationId AND s.sessionStatus = 'PUBLISHED' AND s.startAt >= CURRENT_TIMESTAMP ORDER BY s.startAt ASC")
    List<DonationSession> findUpcomingByLocationId(@Param("locationId") UUID locationId, Pageable pageable);
}
