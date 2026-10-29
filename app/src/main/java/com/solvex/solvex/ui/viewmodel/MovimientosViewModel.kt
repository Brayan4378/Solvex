package com.solvex.solvex.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solvex.solvex.data.AppDatabase
import com.solvex.solvex.data.MovimientoEntity
import com.solvex.solvex.data.SessionManager
import com.solvex.solvex.data.repository.MovimientoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// viewmodel que maneja los movimientos (gastos e ingresos) del usuario
class MovimientosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MovimientoRepository
    private val sessionManager = SessionManager(application)

    private val _userEmail = MutableStateFlow(sessionManager.obtenerEmail())
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    // estado del filtro (0 = todos, 1 = solo gastos, 2 = solo ingresos)
    private val _filtroSeleccionado = MutableStateFlow(0)
    val filtroSeleccionado: StateFlow<Int> = _filtroSeleccionado.asStateFlow()

    // estado para mostrar el dialogo de formulario
    private val _mostrarDialogoFormulario = MutableStateFlow(false)
    val mostrarDialogoFormulario: StateFlow<Boolean> = _mostrarDialogoFormulario.asStateFlow()

    // movimiento que se esta editando (null = nuevo)
    private val _movimientoAEditar = MutableStateFlow<MovimientoEntity?>(null)
    val movimientoAEditar: StateFlow<MovimientoEntity?> = _movimientoAEditar.asStateFlow()

    // movimiento a eliminar
    private val _movimientoAEliminar = MutableStateFlow<MovimientoEntity?>(null)
    val movimientoAEliminar: StateFlow<MovimientoEntity?> = _movimientoAEliminar.asStateFlow()

    // datos reactivos desde Room
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

    fun cambiarFiltro(filtro: Int) {
        _filtroSeleccionado.value = filtro
    }

    fun abrirFormularioNuevo() {
        _movimientoAEditar.value = null
        _mostrarDialogoFormulario.value = true
    }

    fun abrirFormularioEditar(movimiento: MovimientoEntity) {
        _movimientoAEditar.value = movimiento
        _mostrarDialogoFormulario.value = true
    }

    fun cerrarFormulario() {
        _mostrarDialogoFormulario.value = false
    }

    fun solicitarEliminar(movimiento: MovimientoEntity) {
        _movimientoAEliminar.value = movimiento
    }

    fun cancelarEliminar() {
        _movimientoAEliminar.value = null
    }

    fun guardarMovimiento(movimiento: MovimientoEntity) {
        viewModelScope.launch {
            if (_movimientoAEditar.value != null) {
                repository.actualizar(movimiento)
            } else {
                repository.insertar(movimiento)
            }
            _mostrarDialogoFormulario.value = false
        }
    }

    fun confirmarEliminar() {
        val item = _movimientoAEliminar.value ?: return
        viewModelScope.launch {
            repository.eliminar(item)
            _movimientoAEliminar.value = null
        }
    }
}
