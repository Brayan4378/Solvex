package com.solvex.api.dto;

import jakarta.validation.constraints.PositiveOrZero;

public final class CuentaDtos {
    private CuentaDtos() { }

    public record Request(@PositiveOrZero double saldoInicial, @PositiveOrZero double saldoFinal,
                          @PositiveOrZero double totalIngresos, @PositiveOrZero double totalGastos,
                          String banco) { }
    public record Response(String userEmail, double saldoInicial, double saldoFinal,
                           double totalIngresos, double totalGastos, String banco) { }
}
