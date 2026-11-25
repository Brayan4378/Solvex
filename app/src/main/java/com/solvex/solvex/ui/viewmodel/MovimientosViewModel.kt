package com.solvex.solvex.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solvex.solvex.data.entity.MovimientoEntity
import com.solvex.solvex.data.repository.MovimientoRepository
import com.solvex.solvex.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException

// ViewModel que maneja los movimientos (gastos e ingresos) del usuario
class MovimientosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MovimientoRepository(application)
    private val sessionManager = SessionManager(application)

    val cargando: StateFlow<Boolean> = repository.cargando
    val errorCarga: StateFlow<String> = repository.errorCarga
    private val _errorOperacion = MutableStateFlow("")
    val errorOperacion: StateFlow<String> = _errorOperacion.asStateFlow()
    private val _operacionEnCurso = MutableStateFlow(false)
    val operacionEnCurso: StateFlow<Boolean> = _operacionEnCurso.asStateFlow()

    private val _userEmail = MutableStateFlow(sessionManager.obtenerEmail())
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    // Estado del filtro (0 = todos, 1 = solo gastos, 2 = solo ingresos)
    private val _filtroSeleccionado = MutableStateFlow(0)
    val filtroSeleccionado: StateFlow<Int> = _filtroSeleccionado.asStateFlow()

    // Estado para mostrar el diálogo de formulario
    private val _mostrarDialogoFormulario = MutableStateFlow(false)
    val mostrarDialogoFormulario: StateFlow<Boolean> = _mostrarDialogoFormulario.asStateFlow()

    // Movimiento que se está editando (null = nuevo)
    private val _movimientoAEditar = MutableStateFlow<MovimientoEntity?>(null)
    val movimientoAEditar: StateFlow<MovimientoEntity?> = _movimientoAEditar.asStateFlow()

    // Movimiento individual a eliminar
    private val _movimientoAEliminar = MutableStateFlow<MovimientoEntity?>(null)
    val movimientoAEliminar: StateFlow<MovimientoEntity?> = _movimientoAEliminar.asStateFlow()

    // Estado para diálogo de confirmación de eliminación masiva
    private val _mostrarDialogoEliminarTodo = MutableStateFlow(false)
    val mostrarDialogoEliminarTodo: StateFlow<Boolean> = _mostrarDialogoEliminarTodo.asStateFlow()

    // Datos reactivos desde Firestore
    val movimientos: StateFlow<List<MovimientoEntity>> = _userEmail.flatMapLatest { email ->
        repository.obtenerPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalGastos: StateFlow<Double> = _userEmail.flatMapLatest { email ->
        repository.obtenerTotalGastosPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIngresos: StateFlow<Double> = _userEmail.flatMapLatest { email ->
        repository.obtenerTotalIngresosPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun actualizarEmail(email: String) {
        _userEmail.value = email
        repository.actualizarAhora()
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

    fun solicitarEliminarTodo() {
        _mostrarDialogoEliminarTodo.value = true
    }

    fun cancelarEliminarTodo() {
        _mostrarDialogoEliminarTodo.value = false
    }

    fun guardarMovimiento(movimiento: MovimientoEntity) {
        if (_operacionEnCurso.value) return
        _operacionEnCurso.value = true
        _errorOperacion.value = ""
        viewModelScope.launch {
            try {
                if (_movimientoAEditar.value != null) {
                    repository.actualizar(movimiento)
                } else {
                    repository.insertar(movimiento)
                }
                _mostrarDialogoFormulario.value = false
            } catch (exception: Exception) {
                _errorOperacion.value = mensajeError(exception)
            } finally {
                _operacionEnCurso.value = false
            }
        }
    }

    fun confirmarEliminar() {
        val item = _movimientoAEliminar.value ?: return
        if (_operacionEnCurso.value) return
        _operacionEnCurso.value = true
        _errorOperacion.value = ""
        viewModelScope.launch {
            try {
                repository.eliminar(item)
                _movimientoAEliminar.value = null
            } catch (exception: Exception) {
                _errorOperacion.value = mensajeError(exception)
            } finally {
                _operacionEnCurso.value = false
            }
        }
    }

    fun confirmarEliminarTodo() {
        if (_operacionEnCurso.value) return
        _operacionEnCurso.value = true
        _errorOperacion.value = ""
        viewModelScope.launch {
            try {
                repository.eliminarTodosPorUsuario(_userEmail.value)
                _mostrarDialogoEliminarTodo.value = false
            } catch (exception: Exception) {
                _errorOperacion.value = mensajeError(exception)
                _mostrarDialogoEliminarTodo.value = false
            } finally {
                _operacionEnCurso.value = false
            }
        }
    }

    fun limpiarErrorOperacion() {
        _errorOperacion.value = ""
    }

    private fun mensajeError(exception: Exception): String = when (exception) {
        is HttpException -> when (exception.code()) {
            401 -> "Tu sesión venció. Inicia sesión nuevamente."
            403 -> "El servidor rechazó el acceso. Inicia sesión nuevamente."
            in 500..599 -> "El servidor tuvo un error. Intenta de nuevo en unos segundos."
            else -> "No se pudo completar la operación (HTTP ${exception.code()})."
        }
        is java.io.IOException -> "No hay conexión con el servidor. Comprueba que esté activo e intenta de nuevo."
        else -> exception.localizedMessage ?: "No se pudo completar la operación. Intenta de nuevo."
    }
}
