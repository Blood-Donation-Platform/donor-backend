package pt.sanguept.identity.dtos;

import lombok.Builder;

@Builder
public record LoginResponse(String accessToken, String refreshToken, long expiresInSeconds) { }
