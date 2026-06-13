package pt.sanguept.notification.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.notification.dtos.NotificationPreferenceDto;
import pt.sanguept.notification.entities.NotificationPreference;
import pt.sanguept.notification.mappers.NotificationPreferenceMapper;
import pt.sanguept.notification.repositories.NotificationPreferenceRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    public NotificationPreferenceDto get(UUID userId) {
        NotificationPreference entity = repository.findByUserId(userId)
                .orElseGet(() -> createDefault(userId));
        return NotificationPreferenceMapper.toDto(entity);
    }

    @Transactional
    public NotificationPreferenceDto update(UUID userId, NotificationPreferenceDto dto) {
        NotificationPreference entity = repository.findByUserId(userId)
                .orElseGet(() -> createDefault(userId));
        entity.setEnabled(dto.enabled());
        entity.setMuteUntil(dto.muteUntil());
        return NotificationPreferenceMapper.toDto(repository.save(entity));
    }

    @Transactional
    public NotificationPreferenceDto mute(UUID userId, Instant until) {
        NotificationPreference entity = repository.findByUserId(userId)
                .orElseGet(() -> createDefault(userId));
        entity.setMuteUntil(until);
        return NotificationPreferenceMapper.toDto(repository.save(entity));
    }

    @Transactional
    public NotificationPreferenceDto unmute(UUID userId) {
        NotificationPreference entity = repository.findByUserId(userId)
                .orElseGet(() -> createDefault(userId));
        entity.setMuteUntil(null);
        return NotificationPreferenceMapper.toDto(repository.save(entity));
    }

    @Transactional
    public NotificationPreferenceDto enable(UUID userId) {
        NotificationPreference entity = repository.findByUserId(userId)
                .orElseGet(() -> createDefault(userId));
        entity.setEnabled(true);
        return NotificationPreferenceMapper.toDto(repository.save(entity));
    }

    @Transactional
    public NotificationPreferenceDto disable(UUID userId) {
        NotificationPreference entity = repository.findByUserId(userId)
                .orElseGet(() -> createDefault(userId));
        entity.setEnabled(false);
        return NotificationPreferenceMapper.toDto(repository.save(entity));
    }

    private NotificationPreference createDefault(UUID userId) {
        return NotificationPreference.builder()
                .userId(userId)
                .enabled(true)
                .build();
    }

}
