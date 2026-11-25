package com.solvex.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() { }

    public record RegisterRequest(@NotBlank @Size(max = 100) String nombre,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 100) String contrasena) { }
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String contrasena) { }
    public record GoogleRequest(@NotBlank String idToken) { }
    public record UserResponse(int id, String nombre, String email) { }
    public record AuthResponse(String accessToken, String tokenType, UserResponse usuario) { }
}
