package com.solvex.solvex.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.solvex.solvex.data.entity.UsuarioEntity
import kotlinx.coroutines.tasks.await

// repositorio que conecta usuarios con Cloud Firestore
class FirestoreUsuarioRepository {

    private val db = FirebaseFirestore.getInstance()
    private val coleccion = db.collection("usuarios")

    suspend fun insertar(usuario: UsuarioEntity) {
        val docRef = coleccion.document(usuario.email.trim().lowercase())
        val datos = hashMapOf(
            "id" to usuario.id,
            "nombre" to usuario.nombre,
            "email" to usuario.email.trim().lowercase(),
            "contrasena" to usuario.contrasena
        )
        docRef.set(datos).await()
    }

    suspend fun login(email: String, contrasena: String): UsuarioEntity? {
        val doc = coleccion.document(email.trim().lowercase()).get().await()
        if (doc.exists()) {
            val passGuardada = doc.getString("contrasena")
            if (passGuardada == contrasena) {
                return UsuarioEntity(
                    id = doc.getLong("id")?.toInt() ?: 0,
                    nombre = doc.getString("nombre") ?: "",
                    email = doc.getString("email") ?: email,
                    contrasena = passGuardada
                )
            }
        }
        return null
    }

    suspend fun existeEmail(email: String): Int {
        val doc = coleccion.document(email.trim().lowercase()).get().await()
        return if (doc.exists()) 1 else 0
    }

    suspend fun buscarPorEmail(email: String): UsuarioEntity? {
        val doc = coleccion.document(email.trim().lowercase()).get().await()
        if (doc.exists()) {
            return UsuarioEntity(
                id = doc.getLong("id")?.toInt() ?: 0,
                nombre = doc.getString("nombre") ?: "",
                email = doc.getString("email") ?: email,
                contrasena = doc.getString("contrasena") ?: ""
            )
        }
        return null
    }
}
