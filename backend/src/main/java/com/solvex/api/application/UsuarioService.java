package com.solvex.api.application;

import com.solvex.api.dto.AuthDtos;
import com.solvex.api.dto.ProfileDtos;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {
    private final AuthService authService;
    public UsuarioService(AuthService authService) { this.authService = authService; }

    public AuthDtos.UserResponse getCurrent(String email) { return authService.profile(email); }
    public AuthDtos.UserResponse update(String email, ProfileDtos.UpdateRequest request) {
        return authService.updateProfile(email, request.nombre());
    }
}
