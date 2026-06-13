package pt.sanguept.notification.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.notification.entities.NotificationPreference;
import pt.sanguept.notification.entities.NotificationSubscription;
import pt.sanguept.notification.enums.SubscriptionType;
import pt.sanguept.notification.repositories.NotificationPreferenceRepository;
import pt.sanguept.notification.repositories.NotificationSubscriptionRepository;
import pt.sanguept.donationsession.entities.DonationSession;
import pt.sanguept.territory.entities.AdministrativeDivision;
import pt.sanguept.territory.services.DivisionService;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultNotificationMatchingService implements NotificationMatchingService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final NotificationSubscriptionRepository subscriptionRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final DivisionService divisionService;

    @Override
    public Set<UUID> findInterestedUsers(DonationSession session) {
        Set<UUID> candidateUserIds = new HashSet<>();

        matchByAdministrativeDivision(session, candidateUserIds);
        matchByRadius(session, candidateUserIds);

        return filterByPreferences(candidateUserIds);
    }

    private void matchByAdministrativeDivision(DonationSession session, Set<UUID> candidateUserIds) {
        AdministrativeDivision sessionDivision = session.getLocation().getAdministrativeDivision();
        Set<UUID> divisionIds = new HashSet<>();
        divisionIds.add(sessionDivision.getId());
        for (AdministrativeDivision ancestor : divisionService.findAncestors(sessionDivision.getId())) {
            divisionIds.add(ancestor.getId());
        }

        List<NotificationSubscription> matching = subscriptionRepository.findMatchingAdminSubscriptions(divisionIds);
        for (NotificationSubscription sub : matching) {
            candidateUserIds.add(sub.getUserId());
        }
    }

    private void matchByRadius(DonationSession session, Set<UUID> candidateUserIds) {
        double sessionLat = session.getLocation().getCoordinates().getY();
        double sessionLon = session.getLocation().getCoordinates().getX();

        List<NotificationSubscription> radiusSubs = subscriptionRepository.findByTypeAndEnabled(
                SubscriptionType.RADIUS, true);

        for (NotificationSubscription sub : radiusSubs) {
            double distance = haversineDistance(
                    sub.getLatitude(), sub.getLongitude(), sessionLat, sessionLon);
            if (distance <= sub.getRadiusKm()) {
                candidateUserIds.add(sub.getUserId());
            }
        }
    }

    private Set<UUID> filterByPreferences(Set<UUID> candidateUserIds) {
        if (candidateUserIds.isEmpty()) {
            return Set.of();
        }

        Instant now = Instant.now();
        Map<UUID, NotificationPreference> prefMap = preferenceRepository.findByUserIdIn(candidateUserIds)
                .stream()
                .collect(Collectors.toMap(NotificationPreference::getUserId, Function.identity()));

        Set<UUID> result = new HashSet<>();
        for (UUID userId : candidateUserIds) {
            NotificationPreference pref = prefMap.get(userId);
            if (pref == null || pref.isEnabled()) {
                if (pref == null || pref.getMuteUntil() == null || pref.getMuteUntil().isBefore(now)) {
                    result.add(userId);
                }
            }
        }

        return result;
    }

    static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

}
