package com.solvex.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public final class MovimientoDtos {
    private MovimientoDtos() { }

    public record Request(@NotBlank String titulo, @PositiveOrZero double monto,
                          @NotBlank String categoria, @NotBlank String tipo, long fecha) { }
    public record Response(int id, String usuarioEmail, String titulo, double monto, String categoria,
                           String tipo, long fecha) { }
}
