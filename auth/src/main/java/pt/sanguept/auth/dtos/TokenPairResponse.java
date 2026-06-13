package pt.sanguept.auth.dtos;

import lombok.Builder;

@Builder
public record TokenPairResponse(String accessToken, String refreshToken) { }
