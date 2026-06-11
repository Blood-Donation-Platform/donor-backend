package pt.sanguept.donationsession.repositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pt.sanguept.commoninfra.repositories.BaseRepository;
import pt.sanguept.donationsession.entities.DonationSession;

import java.util.UUID;

@Repository
public interface DonationSessionRepository extends BaseRepository<DonationSession, UUID>, JpaSpecificationExecutor<DonationSession> {
}
