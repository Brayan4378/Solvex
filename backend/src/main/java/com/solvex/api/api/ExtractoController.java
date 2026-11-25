package com.solvex.api.api;

import com.solvex.api.application.ExtractoService;
import com.solvex.api.dto.ExtractoDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/extractos")
public class ExtractoController {
    private final ExtractoService service;
    public ExtractoController(ExtractoService service) { this.service = service; }

    @GetMapping
    public List<ExtractoDtos.Response> list(JwtAuthenticationToken auth) { return service.list(auth.getName()); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExtractoDtos.Response create(JwtAuthenticationToken auth, @Valid @RequestBody ExtractoDtos.Request request) {
        return service.create(auth.getName(), request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll(JwtAuthenticationToken auth) { service.deleteAll(auth.getName()); }
}
