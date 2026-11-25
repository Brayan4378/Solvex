package com.solvex.api.domain.model;

public record Movimiento(int id, String usuarioEmail, String titulo, double monto,
                         String categoria, String tipo, long fecha) { }
