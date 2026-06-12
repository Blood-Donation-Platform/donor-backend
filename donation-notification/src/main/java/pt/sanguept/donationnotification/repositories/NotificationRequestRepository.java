package pt.sanguept.donationnotification.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.sanguept.donationnotification.entities.NotificationRequest;

import java.util.UUID;

@Repository
public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, UUID> {

    boolean existsByUserIdAndSessionId(UUID userId, UUID sessionId);

}
