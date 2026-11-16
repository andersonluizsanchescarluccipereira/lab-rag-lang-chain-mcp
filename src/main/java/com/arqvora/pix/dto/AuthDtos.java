package com.arqvora.pix.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() { }
    public record LoginRequest(
            @NotBlank @Email @Schema(example = "demo@arqvora.local") String email,
            @NotBlank @Schema(example = "Demo@123456") String password) { }
    public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) { }
}
