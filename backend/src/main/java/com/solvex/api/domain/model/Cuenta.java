package com.solvex.api.domain.model;

public record Cuenta(String userEmail, double saldoInicial, double saldoFinal,
                     double totalIngresos, double totalGastos, String banco) { }
