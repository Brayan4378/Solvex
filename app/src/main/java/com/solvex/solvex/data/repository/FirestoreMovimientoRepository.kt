package com.solvex.solvex.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.solvex.solvex.data.entity.MovimientoEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

// Repositorio de Firestore con soporte de caché en memoria e inserciones en lote (WriteBatch)
class FirestoreMovimientoRepository {

    private val db = FirebaseFirestore.getInstance()
    private val coleccion = db.collection("movimientos")

    companion object {
        // Cache en memoria estático para garantizar respuesta e inserción instantánea
        private val inMemoryCache = mutableListOf<MovimientoEntity>()
    }

    // Escucha en tiempo real los movimientos filtrados por usuario (con deduplicación por firma única)
    fun obtenerPorUsuario(email: String): Flow<List<MovimientoEntity>> = callbackFlow {
        val emailLimpio = if (email.isBlank()) "usuario@solvex.com" else email.trim().lowercase()

        // 1. Enviar de inmediato lo que se encuentra en el caché en memoria
        val local = synchronized(inMemoryCache) {
            inMemoryCache.filter {
                it.usuarioEmail.trim().lowercase() == emailLimpio || it.usuarioEmail.isBlank()
            }.distinctBy { "${it.titulo}_${it.monto}_${it.fecha}" }
            .sortedByDescending { it.fecha }
        }

        trySend(local)

        // 2. Escuchar en tiempo real desde Firestore
        val listener = coleccion
            .whereEqualTo("usuarioEmail", emailLimpio)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(local)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val remotos = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getLong("id")?.toInt() ?: doc.id.hashCode()
                        val usuarioEmail = doc.getString("usuarioEmail") ?: emailLimpio
                        val titulo = doc.getString("titulo") ?: ""
                        val monto = doc.getDouble("monto") ?: 0.0
                        val categoria = doc.getString("categoria") ?: "Otro"
                        val tipo = doc.getString("tipo") ?: "gasto"
                        val fecha = doc.getLong("fecha") ?: System.currentTimeMillis()

                        MovimientoEntity(
                            id = id,
                            usuarioEmail = usuarioEmail,
                            titulo = titulo,
                            monto = monto,
                            categoria = categoria,
                            tipo = tipo,
                            fecha = fecha
                        )
                    }

                    val todos = ArrayList<MovimientoEntity>()
                    todos.addAll(remotos)
                    todos.addAll(local)

                    val combinados = todos
                        .distinctBy { "${it.titulo}_${it.monto}_${it.fecha}" }
                        .sortedByDescending { it.fecha }

                    trySend(if (combinados.isNotEmpty()) combinados else local)
                } else {
                    trySend(local)
                }
            }
        awaitClose { listener.remove() }
    }

    // Escucha en tiempo real el total de gastos calculado directamente sobre la lista deduplicada
    fun obtenerTotalGastosPorUsuario(email: String): Flow<Double> =
        obtenerPorUsuario(email).map { lista ->
            lista.filter { it.tipo == "gasto" }.sumOf { it.monto }
        }

    // Escucha en tiempo real el total de ingresos calculado directamente sobre la lista deduplicada
    fun obtenerTotalIngresosPorUsuario(email: String): Flow<Double> =
        obtenerPorUsuario(email).map { lista ->
            lista.filter { it.tipo == "ingreso" }.sumOf { it.monto }
        }

    // Insertar un único movimiento
    suspend fun insertar(movimiento: MovimientoEntity) {
        val emailLimpio = movimiento.usuarioEmail.ifBlank { "usuario@solvex.com" }
        val movLimpio = movimiento.copy(usuarioEmail = emailLimpio)

        synchronized(inMemoryCache) {
            inMemoryCache.removeAll { it.titulo == movLimpio.titulo && it.monto == movLimpio.monto && it.fecha == movLimpio.fecha }
            inMemoryCache.add(0, movLimpio)
        }

        try {
            val docRef = if (movLimpio.id != 0) coleccion.document(movLimpio.id.toString()) else coleccion.document()
            val datos = hashMapOf(
                "id" to if (movLimpio.id != 0) movLimpio.id else docRef.id.hashCode(),
                "usuarioEmail" to movLimpio.usuarioEmail,
                "titulo" to movLimpio.titulo,
                "monto" to movLimpio.monto,
                "categoria" to movLimpio.categoria,
                "tipo" to movLimpio.tipo,
                "fecha" to movLimpio.fecha
            )
            docRef.set(datos).await()
        } catch (_: Exception) {
            Log.e("FirestoreRepo", "Error al insertar individual")
        }
    }

    // INSERTAR LISTA COMPLETA EN LOTE: Reemplaza o deduplica exactamente sin multiplicar los registros
    suspend fun insertarLista(movimientos: List<MovimientoEntity>) {
        if (movimientos.isEmpty()) return

        val unicos = movimientos.distinctBy { "${it.titulo}_${it.monto}_${it.fecha}" }

        // 1. Reemplazar la lista en memoria con los elementos únicos del extracto
        synchronized(inMemoryCache) {
            inMemoryCache.clear()
            inMemoryCache.addAll(unicos)
        }

        // 2. Guardar en Firestore en lote
        try {
            unicos.chunked(400).forEach { lote ->
                val batch = db.batch()
                for (movimiento in lote) {
                    val docRef = coleccion.document()
                    val docId = if (movimiento.id != 0) movimiento.id else docRef.id.hashCode()

                    val datos = hashMapOf(
                        "id" to docId,
                        "usuarioEmail" to movimiento.usuarioEmail,
                        "titulo" to movimiento.titulo,
                        "monto" to movimiento.monto,
                        "categoria" to movimiento.categoria,
                        "tipo" to movimiento.tipo,
                        "fecha" to movimiento.fecha
                    )
                    batch.set(docRef, datos)
                }
                batch.commit().await()
            }
        } catch (_: Exception) {
            Log.e("FirestoreRepo", "Error al insertar lote en Firestore")
        }
    }

    // Actualizar un movimiento existente
    suspend fun actualizar(movimiento: MovimientoEntity) {
        synchronized(inMemoryCache) {
            val idx = inMemoryCache.indexOfFirst { it.id == movimiento.id }
            if (idx != -1) {
                inMemoryCache[idx] = movimiento
            }
        }

        try {
            val querySnapshot = coleccion.whereEqualTo("id", movimiento.id).get().await()
            if (!querySnapshot.isEmpty) {
                for (doc in querySnapshot.documents) {
                    doc.reference.update(
                        "titulo", movimiento.titulo,
                        "monto", movimiento.monto,
                        "categoria", movimiento.categoria,
                        "tipo", movimiento.tipo
                    ).await()
                }
            }
        } catch (_: Exception) {
            Log.e("FirestoreRepo", "Error al actualizar")
        }
    }

    // Eliminar un movimiento
    suspend fun eliminar(movimiento: MovimientoEntity) {
        synchronized(inMemoryCache) {
            inMemoryCache.removeAll { it.id == movimiento.id || (it.titulo == movimiento.titulo && it.monto == movimiento.monto) }
        }

        try {
            val querySnapshot = coleccion.whereEqualTo("id", movimiento.id).get().await()
            for (doc in querySnapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (_: Exception) {
            Log.e("FirestoreRepo", "Error al eliminar")
        }
    }

    // Eliminar todos los movimientos
    suspend fun eliminarTodosPorUsuario(email: String) {
        val emailLimpio = if (email.isBlank()) "usuario@solvex.com" else email.trim().lowercase()
        synchronized(inMemoryCache) {
            inMemoryCache.removeAll { it.usuarioEmail == emailLimpio || emailLimpio.isBlank() }
        }

        try {
            val querySnapshot = coleccion.whereEqualTo("usuarioEmail", emailLimpio).get().await()
            if (!querySnapshot.isEmpty) {
                val batch = db.batch()
                for (doc in querySnapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
        } catch (_: Exception) {
            Log.e("FirestoreRepo", "Error al eliminar todo")
        }
    }
}
