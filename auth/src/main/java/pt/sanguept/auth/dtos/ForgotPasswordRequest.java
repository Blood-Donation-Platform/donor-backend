package pt.sanguept.auth.dtos;

public record ForgotPasswordRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email String email) {}
