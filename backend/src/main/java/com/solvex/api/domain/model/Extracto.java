package com.solvex.api.domain.model;

public record Extracto(String id, String userEmail, String nombreArchivo, String banco,
                       long fechaProcesado, int totalMovimientos, String estado) { }
