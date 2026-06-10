package pt.sanguept.donationsession.mappers;

import org.springframework.data.domain.Page;
import pt.sanguept.donationsession.dtos.DonationSessionDto;
import pt.sanguept.donationsession.entities.DonationSession;

public class DonationSessionMapper {

    private DonationSessionMapper() {}

    public static DonationSessionDto toDto(DonationSession entity) {
        if (entity == null) return null;
        return DonationSessionDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .locationId(entity.getLocation() != null ? entity.getLocation().getId() : null)
                .locationName(entity.getLocation() != null ? entity.getLocation().getName() : null)
                .startAt(entity.getStartAt())
                .endAt(entity.getEndAt())
                .status(entity.getSessionStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getLastModifiedAt())
                .build();
    }

    public static Page<DonationSessionDto> toDto(Page<DonationSession> page) {
        return page.map(DonationSessionMapper::toDto);
    }
}
