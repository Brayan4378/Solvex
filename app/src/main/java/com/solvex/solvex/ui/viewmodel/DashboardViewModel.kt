package com.solvex.solvex.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.solvex.solvex.data.AppDatabase
import com.solvex.solvex.data.SessionManager
import com.solvex.solvex.data.repository.MovimientoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import androidx.lifecycle.viewModelScope
import com.solvex.solvex.data.MovimientoEntity

// viewmodel que maneja el estado del dashboard
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MovimientoRepository
    private val sessionManager = SessionManager(application)

    private val _userEmail = MutableStateFlow(sessionManager.obtenerEmail())

    // estado para mostrar el dialogo de confirmacion de cierre de sesion
    private val _showLogoutDialog = MutableStateFlow(false)
    val showLogoutDialog: StateFlow<Boolean> = _showLogoutDialog.asStateFlow()

    // datos reactivos del dashboard
    val movimientos: StateFlow<List<MovimientoEntity>> = _userEmail.flatMapLatest { email ->
        repository.obtenerPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalGastos: StateFlow<Double> = _userEmail.flatMapLatest { email ->
        repository.obtenerTotalGastosPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIngresos: StateFlow<Double> = _userEmail.flatMapLatest { email ->
        repository.obtenerTotalIngresosPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        val db = AppDatabase.obtenerInstancia(application)
        repository = MovimientoRepository(db.movimientoDao())
    }

    fun actualizarEmail(email: String) {
        _userEmail.value = email
    }

    fun mostrarDialogoLogout() {
        _showLogoutDialog.value = true
    }

    fun ocultarDialogoLogout() {
        _showLogoutDialog.value = false
    }
}
