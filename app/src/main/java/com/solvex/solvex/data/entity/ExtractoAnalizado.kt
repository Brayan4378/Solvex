package com.solvex.solvex.data.entity

// Datos estructurados del extracto analizado por IA
data class ExtractoAnalizado(
    val nombreBanco: String = "",
    val periodo: String = "",
    val saldoInicial: Double = 0.0,
    val saldoFinal: Double = 0.0,
    val totalIngresosCalculado: Double = 0.0,
    val totalGastosCalculado: Double = 0.0,
    val saldoCuadra: Boolean = false,
    val movimientosExtraidos: List<MovimientoEntity> = emptyList(),
    val resumenIA: String = ""
)
