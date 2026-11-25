package com.solvex.solvex.data.entity

// modelo que representa los usuarios registrados
data class UsuarioEntity(
    val id: Int = 0,
    // nombre completo del usuario
    val nombre: String,
    // correo del usuario (unico)
    val email: String,
    // contrasena del usuario
    val contrasena: String
)
