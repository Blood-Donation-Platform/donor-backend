package pt.sanguept.user.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String rawPassword,
        String firstName,
        String lastName
) { }
