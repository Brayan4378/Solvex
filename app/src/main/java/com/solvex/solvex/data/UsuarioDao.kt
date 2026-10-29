package com.solvex.solvex.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

// interfaz para hacer operaciones con la tabla de usuarios
@Dao
interface UsuarioDao {
    // insertar un nuevo usuario
    @Insert
    suspend fun insertar(usuario: UsuarioEntity)

    // buscar un usuario por correo y contrasena sin importar mayusculas o minusculas
    @Query("SELECT * FROM usuarios WHERE LOWER(email) = LOWER(:email) AND contrasena = :contrasena LIMIT 1")
    suspend fun login(email: String, contrasena: String): UsuarioEntity?

    // ver si ya existe un usuario con ese correo
    @Query("SELECT COUNT(*) FROM usuarios WHERE LOWER(email) = LOWER(:email)")
    suspend fun existeEmail(email: String): Int

    // buscar un usuario solo por correo
    @Query("SELECT * FROM usuarios WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun buscarPorEmail(email: String): UsuarioEntity?
}
