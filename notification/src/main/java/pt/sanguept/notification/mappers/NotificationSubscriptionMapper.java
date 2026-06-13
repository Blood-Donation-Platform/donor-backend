package pt.sanguept.notification.mappers;

import pt.sanguept.notification.dtos.NotificationSubscriptionDto;
import pt.sanguept.notification.entities.NotificationSubscription;

import java.util.List;

public class NotificationSubscriptionMapper {

    private NotificationSubscriptionMapper() {}

    public static NotificationSubscriptionDto toDto(NotificationSubscription entity) {
        if (entity == null) return null;
        return NotificationSubscriptionDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .type(entity.getType())
                .enabled(entity.isEnabled())
                .administrativeDivisionId(entity.getAdministrativeDivisionId())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .radiusKm(entity.getRadiusKm())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static List<NotificationSubscriptionDto> toDto(List<NotificationSubscription> entities) {
        return entities.stream()
                .map(NotificationSubscriptionMapper::toDto)
                .toList();
    }

}
