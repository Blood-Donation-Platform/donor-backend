package pt.sanguept.identity.dtos;

import lombok.Builder;

@Builder
public record LoginRequest(String email, String password) { }
