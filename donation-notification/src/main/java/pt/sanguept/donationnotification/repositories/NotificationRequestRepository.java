package pt.sanguept.donationnotification.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pt.sanguept.donationnotification.entities.NotificationRequest;
import pt.sanguept.donationnotification.enums.NotificationRequestStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, UUID> {

    boolean existsByUserIdAndSessionId(UUID userId, UUID sessionId);

    @Query("SELECT r FROM NotificationRequest r WHERE r.status = :status ORDER BY r.createdAt ASC")
    List<NotificationRequest> findPending(NotificationRequestStatus status, Pageable pageable);

}
