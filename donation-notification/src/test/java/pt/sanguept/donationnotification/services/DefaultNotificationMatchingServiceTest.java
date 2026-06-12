package pt.sanguept.donationnotification.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.sanguept.donationlocation.entities.DonationLocation;
import pt.sanguept.donationnotification.entities.NotificationPreference;
import pt.sanguept.donationnotification.entities.NotificationSubscription;
import pt.sanguept.donationnotification.enums.SubscriptionType;
import pt.sanguept.donationnotification.repositories.NotificationPreferenceRepository;
import pt.sanguept.donationnotification.repositories.NotificationSubscriptionRepository;
import pt.sanguept.donationsession.entities.DonationSession;
import pt.sanguept.territory.entities.AdministrativeDivision;
import pt.sanguept.territory.services.DivisionService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultNotificationMatchingServiceTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock
    private NotificationSubscriptionRepository subscriptionRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private DivisionService divisionService;

    @InjectMocks
    private DefaultNotificationMatchingService matchingService;

    private static final UUID USER_AVEIRO = UUID.randomUUID();
    private static final UUID USER_PORTO = UUID.randomUUID();
    private static final UUID USER_RADIUS = UUID.randomUUID();
    private static final UUID USER_MUTED = UUID.randomUUID();
    private static final UUID USER_DISABLED = UUID.randomUUID();
    private static final UUID DIV_AVEIRO = UUID.randomUUID();
    private static final UUID DIV_AVEIRO_PARENT = UUID.randomUUID();
    private static final UUID DIV_PORTUGAL = UUID.randomUUID();
    private static final UUID DIV_PORTO = UUID.randomUUID();

    @Test
    void shouldMatchUserWithAdminSubscriptionForSameDivision() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var aveiroDiv = adminDivision(DIV_AVEIRO);
        var sub = adminSubscription(USER_AVEIRO, DIV_AVEIRO);
        setupAdminMatching(session, aveiroDiv, Set.of(DIV_AVEIRO), List.of(sub));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).contains(USER_AVEIRO);
    }

    @Test
    void shouldNotMatchUserWithAdminSubscriptionForDifferentDivision() {
        var session = sessionAt(DIV_PORTO, 41.15, -8.62);
        var portoDiv = adminDivision(DIV_PORTO);
        when(divisionService.findAncestors(DIV_PORTO)).thenReturn(List.of());
        when(subscriptionRepository.findMatchingAdminSubscriptions(Set.of(DIV_PORTO)))
                .thenReturn(List.of());

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldMatchUserWithSubscriptionForParentDivision() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var aveiroDiv = adminDivision(DIV_AVEIRO);
        var parentDiv = adminDivision(DIV_AVEIRO_PARENT);
        var sub = adminSubscription(USER_PORTO, DIV_AVEIRO_PARENT);
        setupAdminMatchingWithAncestors(session, aveiroDiv, Set.of(DIV_AVEIRO, DIV_AVEIRO_PARENT),
                List.of(parentDiv), List.of(sub));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).contains(USER_PORTO);
    }

    @Test
    void shouldMatchUserWithSubscriptionForPortugal() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var aveiroDiv = adminDivision(DIV_AVEIRO);
        var aveiroParent = adminDivision(DIV_AVEIRO_PARENT);
        var portugalDiv = adminDivision(DIV_PORTUGAL);
        var sub = adminSubscription(USER_PORTO, DIV_PORTUGAL);
        when(divisionService.findAncestors(DIV_AVEIRO)).thenReturn(List.of(aveiroParent, portugalDiv));
        when(subscriptionRepository.findMatchingAdminSubscriptions(
                Set.of(DIV_AVEIRO, DIV_AVEIRO_PARENT, DIV_PORTUGAL))).thenReturn(List.of(sub));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).contains(USER_PORTO);
    }

    @Test
    void shouldMatchRadiusSubscriptionWithinRadius() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var sub = radiusSubscription(USER_RADIUS, 40.65, -8.66, 20);
        when(subscriptionRepository.findByTypeAndEnabled(SubscriptionType.RADIUS, true))
                .thenReturn(List.of(sub));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).contains(USER_RADIUS);
    }

    @Test
    void shouldNotMatchRadiusSubscriptionOutsideRadius() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var sub = radiusSubscription(USER_RADIUS, 42.0, -9.0, 5);
        when(subscriptionRepository.findByTypeAndEnabled(SubscriptionType.RADIUS, true))
                .thenReturn(List.of(sub));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldExcludeMutedUser() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var sub = radiusSubscription(USER_MUTED, 40.65, -8.66, 20);
        when(subscriptionRepository.findByTypeAndEnabled(SubscriptionType.RADIUS, true))
                .thenReturn(List.of(sub));
        when(preferenceRepository.findByUserId(USER_MUTED)).thenReturn(Optional.of(
                mutedPreference(USER_MUTED, Instant.now().plusSeconds(3600))));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).doesNotContain(USER_MUTED);
    }

    @Test
    void shouldExcludeUserWithDisabledPreference() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var sub = radiusSubscription(USER_DISABLED, 40.65, -8.66, 20);
        when(subscriptionRepository.findByTypeAndEnabled(SubscriptionType.RADIUS, true))
                .thenReturn(List.of(sub));
        when(preferenceRepository.findByUserId(USER_DISABLED)).thenReturn(Optional.of(
                disabledPreference(USER_DISABLED)));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).doesNotContain(USER_DISABLED);
    }

    @Test
    void shouldIncludeUserWithExpiredMute() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var sub = radiusSubscription(USER_RADIUS, 40.65, -8.66, 20);
        when(subscriptionRepository.findByTypeAndEnabled(SubscriptionType.RADIUS, true))
                .thenReturn(List.of(sub));
        when(preferenceRepository.findByUserId(USER_RADIUS)).thenReturn(Optional.of(
                mutedPreference(USER_RADIUS, Instant.now().minusSeconds(3600))));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).contains(USER_RADIUS);
    }

    private void setupAdminMatching(DonationSession session, AdministrativeDivision division,
                                    Set<UUID> divisionIds, List<NotificationSubscription> subs) {
        when(subscriptionRepository.findMatchingAdminSubscriptions(divisionIds)).thenReturn(subs);
    }

    private void setupAdminMatchingWithAncestors(DonationSession session, AdministrativeDivision division,
                                                  Set<UUID> divisionIds,
                                                  List<AdministrativeDivision> ancestors,
                                                  List<NotificationSubscription> subs) {
        when(divisionService.findAncestors(division.getId())).thenReturn(ancestors);
        when(subscriptionRepository.findMatchingAdminSubscriptions(divisionIds)).thenReturn(subs);
    }

    private DonationSession sessionAt(UUID divisionId, double lat, double lon) {
        var location = new DonationLocation();
        location.setCoordinates(GF.createPoint(new Coordinate(lon, lat)));
        var division = adminDivision(divisionId);
        location.setAdministrativeDivision(division);
        var session = new DonationSession();
        session.setId(UUID.randomUUID());
        session.setLocation(location);
        return session;
    }

    private AdministrativeDivision adminDivision(UUID id) {
        var division = new AdministrativeDivision();
        division.setId(id);
        division.setName("Division-" + id.toString().substring(0, 8));
        return division;
    }

    private NotificationSubscription adminSubscription(UUID userId, UUID divisionId) {
        var sub = new NotificationSubscription();
        sub.setId(UUID.randomUUID());
        sub.setUserId(userId);
        sub.setType(SubscriptionType.ADMINISTRATIVE_DIVISION);
        sub.setEnabled(true);
        sub.setAdministrativeDivisionId(divisionId);
        return sub;
    }

    private NotificationSubscription radiusSubscription(UUID userId, double lat, double lon, int radiusKm) {
        var sub = new NotificationSubscription();
        sub.setId(UUID.randomUUID());
        sub.setUserId(userId);
        sub.setType(SubscriptionType.RADIUS);
        sub.setEnabled(true);
        sub.setLatitude(lat);
        sub.setLongitude(lon);
        sub.setRadiusKm(radiusKm);
        return sub;
    }

    private NotificationPreference mutedPreference(UUID userId, Instant muteUntil) {
        return NotificationPreference.builder()
                .userId(userId)
                .enabled(true)
                .muteUntil(muteUntil)
                .build();
    }

    private NotificationPreference disabledPreference(UUID userId) {
        return NotificationPreference.builder()
                .userId(userId)
                .enabled(false)
                .build();
    }
}
