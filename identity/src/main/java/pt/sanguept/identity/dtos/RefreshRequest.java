package pt.sanguept.identity.dtos;

import lombok.Builder;

@Builder
public record RefreshRequest(String refreshToken) { }
