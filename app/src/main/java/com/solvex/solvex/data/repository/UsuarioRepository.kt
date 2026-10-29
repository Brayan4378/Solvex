package com.solvex.solvex.data.repository

import com.solvex.solvex.data.UsuarioDao
import com.solvex.solvex.data.UsuarioEntity

// repositorio que encapsula el acceso a datos de usuarios
class UsuarioRepository(private val dao: UsuarioDao) {

    suspend fun insertar(usuario: UsuarioEntity) = dao.insertar(usuario)

    suspend fun login(email: String, contrasena: String): UsuarioEntity? =
        dao.login(email, contrasena)

    suspend fun existeEmail(email: String): Int = dao.existeEmail(email)

    suspend fun buscarPorEmail(email: String): UsuarioEntity? = dao.buscarPorEmail(email)
}
