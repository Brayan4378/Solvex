package com.solvex.solvex.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// tabla que guarda los usuarios registrados
@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // nombre completo del usuario
    val nombre: String,
    // correo del usuario (unico)
    val email: String,
    // contrasena del usuario (en texto plano para simplicidad)
    val contrasena: String
)
