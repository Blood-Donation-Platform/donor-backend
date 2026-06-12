package pt.sanguept.donationnotification.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.sanguept.commoninfra.repositories.BaseRepository;
import pt.sanguept.donationnotification.entities.NotificationSubscription;
import pt.sanguept.donationnotification.enums.SubscriptionType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface NotificationSubscriptionRepository
        extends BaseRepository<NotificationSubscription, UUID>, JpaSpecificationExecutor<NotificationSubscription> {

    List<NotificationSubscription> findByUserId(UUID userId);

    List<NotificationSubscription> findByUserIdAndEnabled(UUID userId, boolean enabled);

    List<NotificationSubscription> findByTypeAndEnabled(SubscriptionType type, boolean enabled);

    boolean existsByUserIdAndAdministrativeDivisionIdAndType(UUID userId, UUID administrativeDivisionId, SubscriptionType type);

    boolean existsByUserIdAndLatitudeAndLongitudeAndRadiusKm(UUID userId, double latitude, double longitude, int radiusKm);

    @Query("SELECT ns FROM NotificationSubscription ns " +
            "WHERE ns.type = 'ADMINISTRATIVE_DIVISION' " +
            "AND ns.enabled = true " +
            "AND ns.administrativeDivisionId IN :divisionIds")
    List<NotificationSubscription> findMatchingAdminSubscriptions(@Param("divisionIds") Set<UUID> divisionIds);

}
