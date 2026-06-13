package pt.sanguept.notification.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.notification.dtos.NotificationSubscriptionDto;
import pt.sanguept.notification.dtos.NotificationSubscriptionRequestDto;
import pt.sanguept.notification.entities.NotificationSubscription;
import pt.sanguept.notification.enums.SubscriptionType;
import pt.sanguept.notification.mappers.NotificationSubscriptionMapper;
import pt.sanguept.notification.repositories.NotificationSubscriptionRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSubscriptionService {

    private final NotificationSubscriptionRepository repository;

    public List<NotificationSubscriptionDto> list(UUID userId) {
        return NotificationSubscriptionMapper.toDto(repository.findByUserId(userId));
    }

    @Transactional
    public NotificationSubscriptionDto create(UUID userId, NotificationSubscriptionRequestDto dto) {
        validateSubscriptionRequest(dto);

        if (dto.type() == SubscriptionType.ADMINISTRATIVE_DIVISION) {
            if (repository.existsByUserIdAndAdministrativeDivisionIdAndType(
                    userId, dto.administrativeDivisionId(), dto.type())) {
                throw new IllegalArgumentException("User already has a subscription for this administrative division");
            }
        } else {
            if (repository.existsByUserIdAndLatitudeAndLongitudeAndRadiusKm(
                    userId, dto.latitude(), dto.longitude(), dto.radiusKm())) {
                throw new IllegalArgumentException("User already has an identical radius subscription");
            }
        }

        NotificationSubscription entity = new NotificationSubscription();
        entity.setUserId(userId);
        entity.setType(dto.type());
        entity.setEnabled(dto.enabled());

        if (dto.type() == SubscriptionType.ADMINISTRATIVE_DIVISION) {
            entity.setAdministrativeDivisionId(dto.administrativeDivisionId());
        } else {
            entity.setLatitude(dto.latitude());
            entity.setLongitude(dto.longitude());
            entity.setRadiusKm(dto.radiusKm());
        }

        return NotificationSubscriptionMapper.toDto(repository.save(entity));
    }

    @Transactional
    public NotificationSubscriptionDto update(UUID id, UUID userId, NotificationSubscriptionRequestDto dto) {
        NotificationSubscription entity = findOwned(id, userId);
        validateSubscriptionRequest(dto);

        entity.setType(dto.type());
        entity.setEnabled(dto.enabled());

        if (dto.type() == SubscriptionType.ADMINISTRATIVE_DIVISION) {
            entity.setAdministrativeDivisionId(dto.administrativeDivisionId());
            entity.setLatitude(null);
            entity.setLongitude(null);
            entity.setRadiusKm(null);
        } else {
            entity.setLatitude(dto.latitude());
            entity.setLongitude(dto.longitude());
            entity.setRadiusKm(dto.radiusKm());
            entity.setAdministrativeDivisionId(null);
        }

        return NotificationSubscriptionMapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        NotificationSubscription entity = findOwned(id, userId);
        entity.markDeleted();
        repository.save(entity);
    }

    @Transactional
    public NotificationSubscriptionDto enable(UUID id, UUID userId) {
        NotificationSubscription entity = findOwned(id, userId);
        entity.setEnabled(true);
        return NotificationSubscriptionMapper.toDto(repository.save(entity));
    }

    @Transactional
    public NotificationSubscriptionDto disable(UUID id, UUID userId) {
        NotificationSubscription entity = findOwned(id, userId);
        entity.setEnabled(false);
        return NotificationSubscriptionMapper.toDto(repository.save(entity));
    }

    private NotificationSubscription findOwned(UUID id, UUID userId) {
        NotificationSubscription entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + id));
        if (!entity.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Subscription does not belong to user");
        }
        return entity;
    }

    private void validateSubscriptionRequest(NotificationSubscriptionRequestDto dto) {
        if (dto.type() == SubscriptionType.ADMINISTRATIVE_DIVISION) {
            if (dto.administrativeDivisionId() == null) {
                throw new IllegalArgumentException("administrativeDivisionId is required for ADMINISTRATIVE_DIVISION subscription");
            }
            if (dto.latitude() != null || dto.longitude() != null || dto.radiusKm() != null) {
                throw new IllegalArgumentException("Radius fields must not be set for ADMINISTRATIVE_DIVISION subscription");
            }
        } else {
            if (dto.latitude() == null || dto.longitude() == null || dto.radiusKm() == null) {
                throw new IllegalArgumentException("latitude, longitude and radiusKm are required for RADIUS subscription");
            }
            if (dto.administrativeDivisionId() != null) {
                throw new IllegalArgumentException("administrativeDivisionId must not be set for RADIUS subscription");
            }
            if (dto.radiusKm() <= 0) {
                throw new IllegalArgumentException("radiusKm must be positive");
            }
        }
    }
}
