package pt.sanguept.identity.dtos;

import lombok.Builder;

@Builder
public record TokenPairResponse(String accessToken, String refreshToken) { }
