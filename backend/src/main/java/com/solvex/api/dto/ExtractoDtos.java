package com.solvex.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public final class ExtractoDtos {
    private ExtractoDtos() { }

    public record Request(@NotBlank String nombreArchivo, @NotBlank String banco,
                          @PositiveOrZero int totalMovimientos, String estado) { }
    public record Response(String id, String nombreArchivo, String banco, long fechaProcesado,
                           int totalMovimientos, String estado) { }
}
