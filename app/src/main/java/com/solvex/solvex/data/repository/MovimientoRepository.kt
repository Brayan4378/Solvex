package com.solvex.solvex.data.repository

import android.content.Context
import com.solvex.solvex.data.demo.DemoDataStore
import com.solvex.solvex.data.entity.MovimientoEntity
import com.solvex.solvex.data.remote.MovimientoRequest
import com.solvex.solvex.data.remote.SolvexApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

/** Repositorio Android: el backend es dueño del acceso a Firestore. */
class MovimientoRepository(context: Context) {
    private val api = SolvexApiClient.get(context.applicationContext)
    private val sync = sharedSync(context.applicationContext)

    val cargando: StateFlow<Boolean> = sync.cargando
    val errorCarga: StateFlow<String> = sync.errorCarga

    fun actualizarAhora() = sync.solicitarActualizacion()

    fun obtenerPorUsuario(email: String): Flow<List<MovimientoEntity>> = if (DemoDataStore.isDemo(email)) {
        DemoDataStore.movimientos
    } else sync.movimientos.map { lista -> lista.map { it.copy(usuarioEmail = email) } }

    fun obtenerTotalGastosPorUsuario(email: String): Flow<Double> = obtenerPorUsuario(email)
        .map { lista -> lista.filter { it.tipo == "gasto" }.sumOf { it.monto } }

    fun obtenerTotalIngresosPorUsuario(email: String): Flow<Double> = obtenerPorUsuario(email)
        .map { lista -> lista.filter { it.tipo == "ingreso" }.sumOf { it.monto } }

    suspend fun insertar(movimiento: MovimientoEntity) {
        if (DemoDataStore.isDemo(movimiento.usuarioEmail)) {
            DemoDataStore.guardar(movimiento)
            return
        }
        api.createMovimiento(movimiento.toRequest())
        actualizarAhora()
    }

    suspend fun insertarLista(movimientos: List<MovimientoEntity>) {
        if (movimientos.isNotEmpty()) {
            if (movimientos.all { DemoDataStore.isDemo(it.usuarioEmail) }) {
                movimientos.forEach(DemoDataStore::guardar)
                return
            }
            api.importMovimientos(movimientos.map { it.toRequest() })
            actualizarAhora()
        }
    }

    suspend fun actualizar(movimiento: MovimientoEntity) {
        if (DemoDataStore.isDemo(movimiento.usuarioEmail)) {
            DemoDataStore.guardar(movimiento)
            return
        }
        api.updateMovimiento(movimiento.id, movimiento.toRequest())
        actualizarAhora()
    }

    suspend fun eliminar(movimiento: MovimientoEntity) {
        if (DemoDataStore.isDemo(movimiento.usuarioEmail)) {
            DemoDataStore.eliminar(movimiento.id)
            return
        }
        val response = api.deleteMovimiento(movimiento.id)
        if (!response.isSuccessful) error("No se pudo eliminar el movimiento")
        actualizarAhora()
    }

    suspend fun eliminarTodosPorUsuario(email: String) {
        if (DemoDataStore.isDemo(email)) {
            DemoDataStore.eliminarTodosMovimientos()
            return
        }
        val response = api.deleteAllMovimientos()
        if (!response.isSuccessful) error("No se pudieron eliminar los movimientos")
        actualizarAhora()
    }

    private fun MovimientoEntity.toRequest() = MovimientoRequest(titulo, monto, categoria, tipo, fecha)

    private class SharedSync(context: Context) {
        private val api = SolvexApiClient.get(context)
        private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
        private val _cargando = MutableStateFlow(true)
        val cargando: StateFlow<Boolean> = _cargando.asStateFlow()
        private val _errorCarga = MutableStateFlow("")
        val errorCarga: StateFlow<String> = _errorCarga.asStateFlow()

        val movimientos = flow {
            var ultimaLista = emptyList<MovimientoEntity>()
            var consultaExitosa = false
            while (currentCoroutineContext().isActive) {
                _cargando.value = !consultaExitosa
                try {
                    ultimaLista = api.getMovimientos()
                    consultaExitosa = true
                    _errorCarga.value = ""
                    emit(ultimaLista)
                } catch (_: Exception) {
                    _errorCarga.value = "No se pudo conectar con el servidor. Reintentando automáticamente…"
                    emit(ultimaLista)
                } finally {
                    _cargando.value = false
                }
                withTimeoutOrNull(POLL_INTERVAL_MS) { refreshRequests.receive() }
            }
        }.shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

        fun solicitarActualizacion() {
            refreshRequests.trySend(Unit)
        }

        companion object {
            private const val POLL_INTERVAL_MS = 5 * 60 * 1_000L
            private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }
    }

    private companion object {
        @Volatile private var instance: SharedSync? = null

        fun sharedSync(context: Context): SharedSync = instance ?: synchronized(this) {
            instance ?: SharedSync(context).also { instance = it }
        }
    }
}
