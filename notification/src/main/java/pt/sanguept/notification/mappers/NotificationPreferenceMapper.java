package pt.sanguept.notification.mappers;

import pt.sanguept.notification.dtos.NotificationPreferenceDto;
import pt.sanguept.notification.entities.NotificationPreference;

public class NotificationPreferenceMapper {

    private NotificationPreferenceMapper() {}

    public static NotificationPreferenceDto toDto(NotificationPreference entity) {
        if (entity == null) return null;
        return NotificationPreferenceDto.builder()
                .enabled(entity.isEnabled())
                .muteUntil(entity.getMuteUntil())
                .build();
    }

}
