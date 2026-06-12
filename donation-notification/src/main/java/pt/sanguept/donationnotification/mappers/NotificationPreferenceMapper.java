package pt.sanguept.donationnotification.mappers;

import pt.sanguept.donationnotification.dtos.NotificationPreferenceDto;
import pt.sanguept.donationnotification.entities.NotificationPreference;

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
