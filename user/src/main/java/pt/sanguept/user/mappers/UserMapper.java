package pt.sanguept.user.mappers;

import pt.sanguept.user.dtos.UserDto;
import pt.sanguept.user.entities.User;

import java.util.List;

public class UserMapper {

    private UserMapper() {}

    public static UserDto toDto(User entity) {
        if (entity == null) return null;
        var roles = entity.getRoles().stream()
                .map(r -> r.getName())
                .sorted()
                .toList();
        return new UserDto(
                entity.getId(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.isEnabled(),
                roles,
                entity.getCreatedAt(),
                entity.getLastModifiedAt()
        );
    }

    public static List<UserDto> toDtoList(List<User> entities) {
        if (entities == null) return List.of();
        return entities.stream().map(UserMapper::toDto).toList();
    }
}
