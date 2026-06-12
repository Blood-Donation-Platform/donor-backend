package pt.sanguept.donationnotification.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query(value = """
        SELECT * FROM notification_request
        WHERE status = :status
          AND (next_attempt_at IS NULL OR next_attempt_at <= NOW())
        ORDER BY created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<NotificationRequest> findPendingForProcessing(@Param("status") String status, @Param("limit") int limit);

    @Query(value = """
        SELECT * FROM notification_request
        WHERE status = 'PROCESSING'
          AND last_attempt_at < NOW() - CAST(:timeoutMinutes || ' minutes' AS INTERVAL)
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<NotificationRequest> findStuckProcessing(@Param("timeoutMinutes") int timeoutMinutes);

    List<NotificationRequest> findByStatusOrderByCreatedAtDesc(NotificationRequestStatus status);

    @Query("SELECT COUNT(r) FROM NotificationRequest r WHERE r.status = :status")
    long countByStatus(@Param("status") NotificationRequestStatus status);

}
