package pt.sanguept.notification.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.sanguept.notification.dtos.NotificationPreferenceDto;
import pt.sanguept.notification.entities.NotificationPreference;
import pt.sanguept.notification.repositories.NotificationPreferenceRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository repository;

    @InjectMocks
    private NotificationPreferenceService service;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void shouldReturnDefaultPreferencesWhenNoneExist() {
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        var result = service.get(USER_ID);

        assertThat(result.enabled()).isTrue();
        assertThat(result.muteUntil()).isNull();
    }

    @Test
    void shouldReturnExistingPreferences() {
        var pref = preference(true, null);
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(pref));

        var result = service.get(USER_ID);

        assertThat(result.enabled()).isTrue();
        assertThat(result.muteUntil()).isNull();
    }

    @Test
    void shouldMuteUser() {
        var pref = preference(true, null);
        var muteUntil = Instant.now().plusSeconds(3600);
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(pref));
        when(repository.save(any())).thenReturn(pref);

        var result = service.mute(USER_ID, muteUntil);

        assertThat(result.muteUntil()).isEqualTo(muteUntil);
    }

    @Test
    void shouldUnmuteUser() {
        var pref = preference(true, Instant.now().plusSeconds(3600));
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(pref));
        when(repository.save(any())).thenReturn(pref);

        var result = service.unmute(USER_ID);

        assertThat(result.muteUntil()).isNull();
    }

    @Test
    void shouldEnablePreferences() {
        var pref = preference(false, null);
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(pref));
        when(repository.save(any())).thenReturn(pref);

        var result = service.enable(USER_ID);

        assertThat(result.enabled()).isTrue();
    }

    @Test
    void shouldDisablePreferences() {
        var pref = preference(true, null);
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(pref));
        when(repository.save(any())).thenReturn(pref);

        var result = service.disable(USER_ID);

        assertThat(result.enabled()).isFalse();
    }

    @Test
    void shouldUpdatePreferences() {
        var pref = preference(true, null);
        var muteUntil = Instant.now().plusSeconds(1800);
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(pref));
        when(repository.save(any())).thenReturn(pref);
        var dto = NotificationPreferenceDto.builder()
                .enabled(false)
                .muteUntil(muteUntil)
                .build();

        var result = service.update(USER_ID, dto);

        assertThat(result.enabled()).isFalse();
        assertThat(result.muteUntil()).isEqualTo(muteUntil);
    }

    private NotificationPreference preference(boolean enabled, Instant muteUntil) {
        return NotificationPreference.builder()
                .userId(USER_ID)
                .enabled(enabled)
                .muteUntil(muteUntil)
                .build();
    }
}
