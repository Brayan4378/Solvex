package com.solvex.solvex.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.solvex.solvex.data.repository.FirestoreMovimientoRepository
import com.solvex.solvex.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class EntidadBancaria(
    val nombre: String,
    val tipoCuenta: String,
    val saldo: Double,
    val icono: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val firestoreRepository = FirestoreMovimientoRepository()
    private val db = FirebaseFirestore.getInstance()

    private val _userEmail = MutableStateFlow(sessionManager.obtenerEmail())

    private val _notificacionesActivas = MutableStateFlow(true)
    val notificacionesActivas: StateFlow<Boolean> = _notificacionesActivas.asStateFlow()

    private val _actualizacionAutoActiva = MutableStateFlow(false)
    val actualizacionAutoActiva: StateFlow<Boolean> = _actualizacionAutoActiva.asStateFlow()

    private val _biometriaActiva = MutableStateFlow(true)
    val biometriaActiva: StateFlow<Boolean> = _biometriaActiva.asStateFlow()

    val movimientos = _userEmail.flatMapLatest { email ->
        firestoreRepository.obtenerPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entidadesVinculadas: StateFlow<List<EntidadBancaria>> = movimientos.map { lista ->
        if (lista.isEmpty()) {
            return@map listOf(
                EntidadBancaria("Bancolombia", "Cuenta de Ahorros", 8450200.0, "bancolombia"),
                EntidadBancaria("Davivienda", "Cuenta de Ahorros", 4120000.0, "davivienda"),
                EntidadBancaria("Nequi", "Depósito de bajo monto", 2280000.0, "nequi")
            )
        }

        val totalIngresos = lista.filter { it.tipo == "ingreso" }.sumOf { it.monto }
        val totalGastos = lista.filter { it.tipo == "gasto" }.sumOf { it.monto }
        val saldoTotal = (totalIngresos - totalGastos).coerceAtLeast(1000000.0)

        listOf(
            EntidadBancaria("Bancolombia", "Cuenta de Ahorros", (saldoTotal * 0.55), "bancolombia"),
            EntidadBancaria("Davivienda", "Cuenta de Ahorros", (saldoTotal * 0.30), "davivienda"),
            EntidadBancaria("Nequi", "Depósito de bajo monto", (saldoTotal * 0.15), "nequi")
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun actualizarEmail(email: String) {
        _userEmail.value = email
    }

    fun toggleNotificaciones(activo: Boolean) {
        _notificacionesActivas.value = activo
    }

    fun toggleActualizacionAuto(activo: Boolean) {
        _actualizacionAutoActiva.value = activo
    }

    fun toggleBiometria(activo: Boolean) {
        _biometriaActiva.value = activo
    }

    fun desvincularTodasLasCuentas(onCompletado: () -> Unit) {
        viewModelScope.launch {
            try {
                val email = _userEmail.value
                val docs = db.collection("extractos").whereEqualTo("userEmail", email).get().await()
                for (doc in docs.documents) {
                    doc.reference.delete().await()
                }
            } catch (_: Exception) {}
            onCompletado()
        }
    }
}
