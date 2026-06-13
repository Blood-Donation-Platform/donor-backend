package pt.sanguept.notification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.sanguept.donationlocation.entities.DonationLocation;
import pt.sanguept.notification.entities.NotificationPreference;
import pt.sanguept.notification.entities.NotificationSubscription;
import pt.sanguept.notification.enums.SubscriptionType;
import pt.sanguept.notification.repositories.NotificationPreferenceRepository;
import pt.sanguept.notification.repositories.NotificationSubscriptionRepository;
import pt.sanguept.donationsession.entities.DonationSession;
import pt.sanguept.territory.dtos.AdministrativeDivisionDto;
import pt.sanguept.territory.entities.AdministrativeDivision;
import pt.sanguept.territory.services.DivisionService;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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

    @BeforeEach
    void setUp() {
        lenient().when(preferenceRepository.findByUserIdIn(any())).thenReturn(List.of());
    }

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
        var parentDiv = adminDivisionDto(DIV_AVEIRO_PARENT);
        var sub = adminSubscription(USER_PORTO, DIV_AVEIRO_PARENT);
        setupAdminMatchingWithAncestors(session, aveiroDiv, Set.of(DIV_AVEIRO, DIV_AVEIRO_PARENT),
                List.of(parentDiv), List.of(sub));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).contains(USER_PORTO);
    }

    @Test
    void shouldMatchUserWithSubscriptionForPortugal() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var aveiroParent = adminDivisionDto(DIV_AVEIRO_PARENT);
        var portugalDiv = adminDivisionDto(DIV_PORTUGAL);
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
        when(preferenceRepository.findByUserIdIn(Set.of(USER_MUTED))).thenReturn(List.of(
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
        when(preferenceRepository.findByUserIdIn(Set.of(USER_DISABLED))).thenReturn(List.of(
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
        when(preferenceRepository.findByUserIdIn(Set.of(USER_RADIUS))).thenReturn(List.of(
                mutedPreference(USER_RADIUS, Instant.now().minusSeconds(3600))));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).contains(USER_RADIUS);
    }

    @Test
    void shouldDeduplicateUserMatchedByBothAdminAndRadius() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var aveiroDiv = adminDivision(DIV_AVEIRO);
        var adminSub = adminSubscription(USER_AVEIRO, DIV_AVEIRO);
        var radiusSub = radiusSubscription(USER_AVEIRO, 40.65, -8.66, 20);
        setupAdminMatching(session, aveiroDiv, Set.of(DIV_AVEIRO), List.of(adminSub));
        when(subscriptionRepository.findByTypeAndEnabled(SubscriptionType.RADIUS, true))
                .thenReturn(List.of(radiusSub));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).containsExactly(USER_AVEIRO);
    }

    @Test
    void shouldDeduplicateUserWithMultipleRadiusSubscriptions() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var sub1 = radiusSubscription(USER_RADIUS, 40.65, -8.66, 20);
        var sub2 = radiusSubscription(USER_RADIUS, 40.63, -8.64, 20);
        when(subscriptionRepository.findByTypeAndEnabled(SubscriptionType.RADIUS, true))
                .thenReturn(List.of(sub1, sub2));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).containsExactly(USER_RADIUS);
    }

    @Test
    void shouldIncludeUserWithNoPreferenceRecord() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var sub = radiusSubscription(USER_RADIUS, 40.65, -8.66, 20);
        when(subscriptionRepository.findByTypeAndEnabled(SubscriptionType.RADIUS, true))
                .thenReturn(List.of(sub));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).contains(USER_RADIUS);
    }

    @Test
    void shouldMatchDifferentUsersViaAdminAndRadiusConcurrently() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var aveiroDiv = adminDivision(DIV_AVEIRO);
        var adminSub = adminSubscription(USER_AVEIRO, DIV_AVEIRO);
        var radiusSub = radiusSubscription(USER_RADIUS, 40.65, -8.66, 20);
        setupAdminMatching(session, aveiroDiv, Set.of(DIV_AVEIRO), List.of(adminSub));
        when(subscriptionRepository.findByTypeAndEnabled(SubscriptionType.RADIUS, true))
                .thenReturn(List.of(radiusSub));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).containsExactlyInAnyOrder(USER_AVEIRO, USER_RADIUS);
    }

    @Test
    void shouldMatchUserAtExactBoundaryOfRadius() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        double distance = DefaultNotificationMatchingService.haversineDistance(
                40.64, -8.65, 40.65, -8.66);
        var sub = radiusSubscription(USER_RADIUS, 40.65, -8.66, (int) Math.ceil(distance));
        when(subscriptionRepository.findByTypeAndEnabled(SubscriptionType.RADIUS, true))
                .thenReturn(List.of(sub));

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).contains(USER_RADIUS);
    }

    @Test
    void shouldHandleEmptyCandidateSet() {
        var session = sessionAt(DIV_AVEIRO, 40.64, -8.65);
        var aveiroDiv = adminDivision(DIV_AVEIRO);
        setupAdminMatching(session, aveiroDiv, Set.of(DIV_AVEIRO), List.of());
        when(subscriptionRepository.findByTypeAndEnabled(SubscriptionType.RADIUS, true))
                .thenReturn(List.of());

        Set<UUID> result = matchingService.findInterestedUsers(session);

        assertThat(result).isEmpty();
    }

    private void setupAdminMatching(DonationSession session, AdministrativeDivision division,
                                    Set<UUID> divisionIds, List<NotificationSubscription> subs) {
        when(subscriptionRepository.findMatchingAdminSubscriptions(divisionIds)).thenReturn(subs);
    }

    private void setupAdminMatchingWithAncestors(DonationSession session, AdministrativeDivision division,
                                                  Set<UUID> divisionIds,
                                                  List<AdministrativeDivisionDto> ancestors,
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

    private AdministrativeDivisionDto adminDivisionDto(UUID id) {
        return new AdministrativeDivisionDto(id, "Division-" + id.toString().substring(0, 8), null, null);
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
