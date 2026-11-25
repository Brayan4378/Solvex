package com.solvex.solvex.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solvex.solvex.data.entity.MovimientoEntity
import com.solvex.solvex.data.repository.CuentaRepository
import com.solvex.solvex.data.repository.MovimientoRepository
import com.solvex.solvex.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MovimientoRepository(application)
    private val sessionManager = SessionManager(application)
    private val cuentaRepository = CuentaRepository(application)

    private val _userEmail = MutableStateFlow(sessionManager.obtenerEmail())

    private val _showLogoutDialog = MutableStateFlow(false)
    val showLogoutDialog: StateFlow<Boolean> = _showLogoutDialog.asStateFlow()

    // Datos reactivos del dashboard desde Firestore
    val movimientos: StateFlow<List<MovimientoEntity>> = _userEmail.flatMapLatest { email ->
        repository.obtenerPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalGastos: StateFlow<Double> = _userEmail.flatMapLatest { email ->
        repository.obtenerTotalGastosPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIngresos: StateFlow<Double> = _userEmail.flatMapLatest { email ->
        repository.obtenerTotalIngresosPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Saldo inicial registrado de la cuenta
    val saldoInicial: StateFlow<Double> = _userEmail.flatMapLatest { email ->
        flow {
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                val saldo = try {
                    cuentaRepository.obtener().saldoInicial.also { sessionManager.guardarSaldoInicial(it) }
                } catch (_: Exception) {
                    sessionManager.obtenerSaldoInicial()
                }
                emit(saldo)
                delay(5 * 60 * 1_000L)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), sessionManager.obtenerSaldoInicial())

    fun actualizarEmail(email: String) {
        _userEmail.value = email
        repository.actualizarAhora()
    }

    fun mostrarDialogoLogout() {
        _showLogoutDialog.value = true
    }

    fun ocultarDialogoLogout() {
        _showLogoutDialog.value = false
    }
}
