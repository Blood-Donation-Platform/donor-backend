package pt.sanguept.notification.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.sanguept.notification.dtos.NotificationSubscriptionRequestDto;
import pt.sanguept.notification.entities.NotificationSubscription;
import pt.sanguept.notification.enums.SubscriptionType;
import pt.sanguept.notification.repositories.NotificationSubscriptionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSubscriptionServiceTest {

    @Mock
    private NotificationSubscriptionRepository repository;

    @InjectMocks
    private NotificationSubscriptionService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SUB_ID = UUID.randomUUID();
    private static final UUID DIV_ID = UUID.randomUUID();

    @Test
    void shouldListUserSubscriptions() {
        var sub = adminSubscription();
        when(repository.findByUserId(USER_ID)).thenReturn(List.of(sub));

        var result = service.list(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(SUB_ID);
    }

    @Test
    void shouldCreateAdminSubscription() {
        var dto = NotificationSubscriptionRequestDto.builder()
                .type(SubscriptionType.ADMINISTRATIVE_DIVISION)
                .enabled(true)
                .administrativeDivisionId(DIV_ID)
                .build();
        when(repository.existsByUserIdAndAdministrativeDivisionIdAndType(USER_ID, DIV_ID, SubscriptionType.ADMINISTRATIVE_DIVISION))
                .thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            NotificationSubscription s = inv.getArgument(0);
            s.setId(SUB_ID);
            return s;
        });

        var result = service.create(USER_ID, dto);

        assertThat(result.type()).isEqualTo(SubscriptionType.ADMINISTRATIVE_DIVISION);
        assertThat(result.administrativeDivisionId()).isEqualTo(DIV_ID);
    }

    @Test
    void shouldRejectDuplicateAdminSubscription() {
        var dto = NotificationSubscriptionRequestDto.builder()
                .type(SubscriptionType.ADMINISTRATIVE_DIVISION)
                .enabled(true)
                .administrativeDivisionId(DIV_ID)
                .build();
        when(repository.existsByUserIdAndAdministrativeDivisionIdAndType(USER_ID, DIV_ID, SubscriptionType.ADMINISTRATIVE_DIVISION))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(USER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has a subscription");
    }

    @Test
    void shouldCreateRadiusSubscription() {
        var dto = NotificationSubscriptionRequestDto.builder()
                .type(SubscriptionType.RADIUS)
                .enabled(true)
                .latitude(40.64)
                .longitude(-8.65)
                .radiusKm(20)
                .build();
        when(repository.existsByUserIdAndLatitudeAndLongitudeAndRadiusKm(USER_ID, 40.64, -8.65, 20))
                .thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            NotificationSubscription s = inv.getArgument(0);
            s.setId(SUB_ID);
            return s;
        });

        var result = service.create(USER_ID, dto);

        assertThat(result.type()).isEqualTo(SubscriptionType.RADIUS);
        assertThat(result.latitude()).isEqualTo(40.64);
    }

    @Test
    void shouldRejectDuplicateRadiusSubscription() {
        var dto = NotificationSubscriptionRequestDto.builder()
                .type(SubscriptionType.RADIUS)
                .enabled(true)
                .latitude(40.64)
                .longitude(-8.65)
                .radiusKm(20)
                .build();
        when(repository.existsByUserIdAndLatitudeAndLongitudeAndRadiusKm(USER_ID, 40.64, -8.65, 20))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(USER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identical radius subscription");
    }

    @Test
    void shouldRejectAdminSubscriptionWithoutDivisionId() {
        var dto = NotificationSubscriptionRequestDto.builder()
                .type(SubscriptionType.ADMINISTRATIVE_DIVISION)
                .enabled(true)
                .build();

        assertThatThrownBy(() -> service.create(USER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("administrativeDivisionId is required");
    }

    @Test
    void shouldRejectAdminSubscriptionWithRadiusFields() {
        var dto = NotificationSubscriptionRequestDto.builder()
                .type(SubscriptionType.ADMINISTRATIVE_DIVISION)
                .enabled(true)
                .administrativeDivisionId(DIV_ID)
                .latitude(40.0)
                .build();

        assertThatThrownBy(() -> service.create(USER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Radius fields must not be set");
    }

    @Test
    void shouldRejectRadiusSubscriptionWithoutCoordinates() {
        var dto = NotificationSubscriptionRequestDto.builder()
                .type(SubscriptionType.RADIUS)
                .enabled(true)
                .radiusKm(20)
                .build();

        assertThatThrownBy(() -> service.create(USER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude, longitude and radiusKm are required");
    }

    @Test
    void shouldRejectRadiusSubscriptionWithDivisionId() {
        var dto = NotificationSubscriptionRequestDto.builder()
                .type(SubscriptionType.RADIUS)
                .enabled(true)
                .latitude(40.64)
                .longitude(-8.65)
                .radiusKm(20)
                .administrativeDivisionId(DIV_ID)
                .build();

        assertThatThrownBy(() -> service.create(USER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("administrativeDivisionId must not be set");
    }

    @Test
    void shouldRejectRadiusWithZeroOrNegativeRadius() {
        var dto = NotificationSubscriptionRequestDto.builder()
                .type(SubscriptionType.RADIUS)
                .enabled(true)
                .latitude(40.64)
                .longitude(-8.65)
                .radiusKm(0)
                .build();

        assertThatThrownBy(() -> service.create(USER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("radiusKm must be positive");
    }

    @Test
    void shouldEnableSubscription() {
        var sub = adminSubscription();
        sub.setEnabled(false);
        when(repository.findById(SUB_ID)).thenReturn(Optional.of(sub));
        when(repository.save(any())).thenReturn(sub);

        var result = service.enable(SUB_ID, USER_ID);

        assertThat(result.enabled()).isTrue();
    }

    @Test
    void shouldDisableSubscription() {
        var sub = adminSubscription();
        sub.setEnabled(true);
        when(repository.findById(SUB_ID)).thenReturn(Optional.of(sub));
        when(repository.save(any())).thenReturn(sub);

        var result = service.disable(SUB_ID, USER_ID);

        assertThat(result.enabled()).isFalse();
    }

    @Test
    void shouldSoftDeleteSubscription() {
        var sub = adminSubscription();
        when(repository.findById(SUB_ID)).thenReturn(Optional.of(sub));

        service.delete(SUB_ID, USER_ID);

        verify(repository).save(sub);
        assertThat(sub.isDeleted()).isTrue();
    }

    @Test
    void shouldRejectAccessToOtherUsersSubscription() {
        var sub = adminSubscription();
        sub.setUserId(UUID.randomUUID());
        when(repository.findById(SUB_ID)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.delete(SUB_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to user");
    }

    private NotificationSubscription adminSubscription() {
        var sub = new NotificationSubscription();
        sub.setId(SUB_ID);
        sub.setUserId(USER_ID);
        sub.setType(SubscriptionType.ADMINISTRATIVE_DIVISION);
        sub.setEnabled(true);
        sub.setAdministrativeDivisionId(DIV_ID);
        return sub;
    }
}
