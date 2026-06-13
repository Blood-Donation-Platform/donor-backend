package pt.sanguept.auth.dtos;

import lombok.Builder;

@Builder
public record RefreshRequest(String refreshToken) { }
