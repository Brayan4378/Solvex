package com.solvex.api.application;

import com.solvex.api.common.ResourceNotFoundException;
import com.solvex.api.domain.model.Movimiento;
import com.solvex.api.dto.MovimientoDtos;
import com.solvex.api.infrastructure.firestore.FirestoreMovimientoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovimientoService {
    private final FirestoreMovimientoRepository repository;
    public MovimientoService(FirestoreMovimientoRepository repository) { this.repository = repository; }

    public List<MovimientoDtos.Response> list(String email) {
        return repository.findByUser(email).stream().map(MovimientoService::toResponse).toList();
    }

    public MovimientoDtos.Response create(String email, MovimientoDtos.Request request) {
        return toResponse(repository.create(email, toDomain(request)));
    }

    public List<MovimientoDtos.Response> importRows(String email, List<MovimientoDtos.Request> requests) {
        if (requests == null || requests.isEmpty()) throw new IllegalArgumentException("El extracto no contiene movimientos");
        return repository.saveImported(email, requests.stream().map(MovimientoService::toDomain).toList())
                .stream().map(MovimientoService::toResponse).toList();
    }

    public MovimientoDtos.Response update(String email, int id, MovimientoDtos.Request request) {
        try { return toResponse(repository.update(email, id, toDomain(request))); }
        catch (IllegalArgumentException exception) { throw new ResourceNotFoundException(exception.getMessage()); }
    }

    public void delete(String email, int id) {
        try { repository.delete(email, id); }
        catch (IllegalArgumentException exception) { throw new ResourceNotFoundException(exception.getMessage()); }
    }

    public void deleteAll(String email) { repository.deleteAll(email); }

    private static Movimiento toDomain(MovimientoDtos.Request request) {
        String tipo = request.tipo().trim().toLowerCase();
        if (!tipo.equals("gasto") && !tipo.equals("ingreso")) throw new IllegalArgumentException("El tipo debe ser gasto o ingreso");
        return new Movimiento(0, "", request.titulo().trim(), request.monto(), request.categoria().trim(), tipo,
                request.fecha() > 0 ? request.fecha() : System.currentTimeMillis());
    }

    private static MovimientoDtos.Response toResponse(Movimiento movement) {
        return new MovimientoDtos.Response(movement.id(), movement.usuarioEmail(), movement.titulo(), movement.monto(),
                movement.categoria(), movement.tipo(), movement.fecha());
    }
}
