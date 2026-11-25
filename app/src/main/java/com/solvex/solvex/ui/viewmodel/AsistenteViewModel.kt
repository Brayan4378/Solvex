package com.solvex.solvex.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solvex.solvex.data.entity.MovimientoEntity
import com.solvex.solvex.data.repository.MovimientoRepository
import com.solvex.solvex.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MensajeChat(
    val texto: String,
    val esUsuario: Boolean,
    val timestamp: String = obtenerHoraActual()
)

private fun obtenerHoraActual(): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.US)
    return sdf.format(Date())
}

@OptIn(ExperimentalCoroutinesApi::class)
class AsistenteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MovimientoRepository(application)
    private val sessionManager = SessionManager(application)
    private var respuestaJob: Job? = null

    private val _userEmail = MutableStateFlow(sessionManager.obtenerEmail())

    val movimientos: StateFlow<List<MovimientoEntity>> = _userEmail.flatMapLatest { email ->
        repository.obtenerPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _mensajes = MutableStateFlow<List<MensajeChat>>(
        listOf(
            MensajeChat(
                texto = "Hola. Soy tu asistente de Solvex. 🟢\n\nPuedo consultar los movimientos de tus extractos: gastos por categoría o comercio, ingresos, balance y transacciones. Por ejemplo: «¿Cuánto gasté en Terpel este mes?».",
                esUsuario = false
            )
        )
    )
    val mensajes: StateFlow<List<MensajeChat>> = _mensajes.asStateFlow()

    fun limpiarHistorial() {
        respuestaJob?.cancel()
        _cargando.value = false
        _mensajes.value = listOf(mensajeBienvenida())
    }

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    fun actualizarEmail(email: String) {
        _userEmail.value = email
        repository.actualizarAhora()
    }

    fun enviarMensaje(texto: String, userName: String = "Carlos") {
        val consulta = texto.trim()
        if (consulta.isBlank()) return

        val mensajeUsuario = MensajeChat(texto = consulta, esUsuario = true)
        _mensajes.value = _mensajes.value + mensajeUsuario
        _cargando.value = true

        respuestaJob = viewModelScope.launch {
            // Firestore puede tardar un momento en entregar la primera instantánea.
            // Evita responder que no hay datos usando el valor inicial vacío del StateFlow.
            val datos = movimientos.value.ifEmpty {
                withTimeoutOrNull(2_000) { movimientos.first { it.isNotEmpty() } } ?: movimientos.value
            }
            val respuesta = generarRespuestaIA(consulta, datos, userName)
            _mensajes.value = _mensajes.value + MensajeChat(texto = respuesta, esUsuario = false)
            _cargando.value = false
        }
    }

    private fun generarRespuestaIA(pregunta: String, movimientos: List<MovimientoEntity>, userName: String): String {
        return AsistenteRespuestaService().responder(pregunta, movimientos, userName)
    }

    private fun mensajeBienvenida() = MensajeChat(
        texto = "Hola. Soy tu asistente de Solvex. 🟢\n\nPuedo consultar los movimientos de tus extractos: gastos por categoría o comercio, ingresos, balance y transacciones. Por ejemplo: «¿Cuánto gasté en Terpel este mes?».",
        esUsuario = false
    )


}
