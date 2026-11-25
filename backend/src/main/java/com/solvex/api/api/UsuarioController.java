package com.solvex.api.api;

import com.solvex.api.application.UsuarioService;
import com.solvex.api.dto.AuthDtos;
import com.solvex.api.dto.ProfileDtos;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios/me")
public class UsuarioController {
    private final UsuarioService service;
    public UsuarioController(UsuarioService service) { this.service = service; }

    @GetMapping
    public AuthDtos.UserResponse getCurrent(JwtAuthenticationToken authentication) {
        return service.getCurrent(authentication.getName());
    }

    @PatchMapping
    public AuthDtos.UserResponse update(JwtAuthenticationToken authentication,
                                       @Valid @RequestBody ProfileDtos.UpdateRequest request) {
        return service.update(authentication.getName(), request);
    }
}
