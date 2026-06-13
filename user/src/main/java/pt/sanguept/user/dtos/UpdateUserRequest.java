package pt.sanguept.user.dtos;

public record UpdateUserRequest(
        String firstName,
        String lastName,
        Boolean enabled
) {}
