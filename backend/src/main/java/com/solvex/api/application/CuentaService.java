package com.solvex.api.application;

import com.solvex.api.domain.model.Cuenta;
import com.solvex.api.dto.CuentaDtos;
import com.solvex.api.infrastructure.firestore.FirestoreCuentaRepository;
import org.springframework.stereotype.Service;

@Service
public class CuentaService {
    private final FirestoreCuentaRepository repository;
    public CuentaService(FirestoreCuentaRepository repository) { this.repository = repository; }

    public CuentaDtos.Response get(String email) {
        return repository.find(email).map(CuentaService::toResponse)
                .orElseGet(() -> new CuentaDtos.Response(email, 0, 0, 0, 0, ""));
    }

    public CuentaDtos.Response save(String email, CuentaDtos.Request request) {
        Cuenta saved = repository.save(email, new Cuenta(email, request.saldoInicial(), request.saldoFinal(),
                request.totalIngresos(), request.totalGastos(), request.banco()));
        return toResponse(saved);
    }

    public void delete(String email) { repository.delete(email); }

    private static CuentaDtos.Response toResponse(Cuenta account) {
        return new CuentaDtos.Response(account.userEmail(), account.saldoInicial(), account.saldoFinal(),
                account.totalIngresos(), account.totalGastos(), account.banco());
    }
}
