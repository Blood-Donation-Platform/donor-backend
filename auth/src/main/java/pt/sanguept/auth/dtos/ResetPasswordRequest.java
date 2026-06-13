package pt.sanguept.auth.dtos;

public record ResetPasswordRequest(@jakarta.validation.constraints.NotBlank String token, @jakarta.validation.constraints.NotBlank String newPassword) {}
