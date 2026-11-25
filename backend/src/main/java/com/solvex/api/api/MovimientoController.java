package com.solvex.api.api;

import com.solvex.api.application.MovimientoService;
import com.solvex.api.dto.MovimientoDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoController {
    private final MovimientoService service;
    public MovimientoController(MovimientoService service) { this.service = service; }

    @GetMapping
    public List<MovimientoDtos.Response> list(JwtAuthenticationToken auth) { return service.list(auth.getName()); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoDtos.Response create(JwtAuthenticationToken auth, @Valid @RequestBody MovimientoDtos.Request request) {
        return service.create(auth.getName(), request);
    }

    @PostMapping("/importar")
    public List<MovimientoDtos.Response> importRows(JwtAuthenticationToken auth,
                                                     @Valid @RequestBody List<MovimientoDtos.Request> rows) {
        return service.importRows(auth.getName(), rows);
    }

    @PutMapping("/{id}")
    public MovimientoDtos.Response update(JwtAuthenticationToken auth, @PathVariable int id,
                                          @Valid @RequestBody MovimientoDtos.Request request) {
        return service.update(auth.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(JwtAuthenticationToken auth, @PathVariable int id) { service.delete(auth.getName(), id); }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll(JwtAuthenticationToken auth) { service.deleteAll(auth.getName()); }
}
