package com.solvex.api.application;

import com.solvex.api.domain.model.Extracto;
import com.solvex.api.dto.ExtractoDtos;
import com.solvex.api.infrastructure.firestore.FirestoreExtractoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExtractoService {
    private final FirestoreExtractoRepository repository;
    public ExtractoService(FirestoreExtractoRepository repository) { this.repository = repository; }

    public List<ExtractoDtos.Response> list(String email) {
        return repository.findByUser(email).stream().map(ExtractoService::toResponse).toList();
    }

    public ExtractoDtos.Response create(String email, ExtractoDtos.Request request) {
        return toResponse(repository.create(email, request.nombreArchivo(), request.banco(),
                request.totalMovimientos(), request.estado()));
    }

    public void deleteAll(String email) { repository.deleteAll(email); }

    private static ExtractoDtos.Response toResponse(Extracto statement) {
        return new ExtractoDtos.Response(statement.id(), statement.nombreArchivo(), statement.banco(),
                statement.fechaProcesado(), statement.totalMovimientos(), statement.estado());
    }
}
