package pt.sanguept.auth.dtos;

import lombok.Builder;

@Builder
public record LoginRequest(String email, String password) { }
