package pt.sanguept.donationnotification.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.sanguept.donationnotification.entities.NotificationPreference;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    Optional<NotificationPreference> findByUserId(UUID userId);

    @Query("SELECT p FROM NotificationPreference p WHERE p.userId IN :userIds")
    List<NotificationPreference> findByUserIdIn(@Param("userIds") Set<UUID> userIds);

}
