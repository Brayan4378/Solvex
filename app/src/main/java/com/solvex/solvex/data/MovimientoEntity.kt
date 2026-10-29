package com.solvex.solvex.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// tabla de room que guarda cada movimiento o gasto asociado a un usuario
@Entity(tableName = "movimientos")
data class MovimientoEntity(
    // identificador unico que se genera automaticamente
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // correo del usuario para que cada persona solo vea sus propios movimientos
    val usuarioEmail: String,
    // descripcion o titulo del movimiento (ejemplo: mercado, transporte, etc.)
    val titulo: String,
    // valor del dinero del movimiento
    val monto: Double,
    // categoria para clasificar el gasto
    val categoria: String,
    // tipo de movimiento: "gasto" o "ingreso"
    val tipo: String = "gasto",
    // fecha en milisegundos para saber cuando se hizo
    val fecha: Long = System.currentTimeMillis()
)
