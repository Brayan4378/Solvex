package com.solvex.api.api;

import com.solvex.api.application.CuentaService;
import com.solvex.api.dto.CuentaDtos;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cuenta")
public class CuentaController {
    private final CuentaService service;
    public CuentaController(CuentaService service) { this.service = service; }

    @GetMapping
    public CuentaDtos.Response get(JwtAuthenticationToken auth) { return service.get(auth.getName()); }

    @PutMapping
    public CuentaDtos.Response save(JwtAuthenticationToken auth, @Valid @RequestBody CuentaDtos.Request request) {
        return service.save(auth.getName(), request);
    }

    @DeleteMapping
    public void delete(JwtAuthenticationToken auth) { service.delete(auth.getName()); }
}
