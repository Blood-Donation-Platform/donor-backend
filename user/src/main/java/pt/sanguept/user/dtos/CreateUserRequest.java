package pt.sanguept.user.dtos;

import lombok.Builder;

@Builder
public record CreateUserRequest(
        String email,
        String rawPassword,
        String firstName,
        String lastName
) { }
