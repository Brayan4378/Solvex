package com.solvex.solvex.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solvex.solvex.data.entity.ExtractoAnalizado
import com.solvex.solvex.data.remote.CuentaResponse
import com.solvex.solvex.data.remote.ExtractoRequest
import com.solvex.solvex.data.repository.CuentaRepository
import com.solvex.solvex.data.repository.ExtractoRepository
import com.solvex.solvex.data.repository.MovimientoRepository
import com.solvex.solvex.data.service.ExtractoAIService
import com.solvex.solvex.data.service.PdfPasswordRequiredException
import com.solvex.solvex.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

// Datos de una carga reciente guardada en Firestore
data class CargaReciente(
    val nombreArchivo: String = "",
    val banco: String = "",
    val fechaProcesado: Long = 0L,
    val totalMovimientos: Int = 0,
    val estado: String = "Procesado"
)

// ViewModel para gestionar la subida de extractos bancarios y análisis con IA (HU4 a HU7)
class ExtractoViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val movimientoRepository = MovimientoRepository(application)
    private val cuentaRepository = CuentaRepository(application)
    private val extractoRepository = ExtractoRepository(application)
    private val aiService = ExtractoAIService()

    private val _userEmail = MutableStateFlow(sessionManager.obtenerEmail())

    // Estado del proceso de análisis
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _nombreArchivo = MutableStateFlow("")
    val nombreArchivo: StateFlow<String> = _nombreArchivo.asStateFlow()

    private val _extractoResultado = MutableStateFlow<ExtractoAnalizado?>(null)
    val extractoResultado: StateFlow<ExtractoAnalizado?> = _extractoResultado.asStateFlow()

    private val _guardadoExitoso = MutableStateFlow(false)
    val guardadoExitoso: StateFlow<Boolean> = _guardadoExitoso.asStateFlow()

    private val _guardandoMovimientos = MutableStateFlow(false)
    val guardandoMovimientos: StateFlow<Boolean> = _guardandoMovimientos.asStateFlow()

    private val _pdfSolicitaContrasena = MutableStateFlow(false)
    val pdfSolicitaContrasena: StateFlow<Boolean> = _pdfSolicitaContrasena.asStateFlow()

    private val _avisoCarga = MutableStateFlow("")
    val avisoCarga: StateFlow<String> = _avisoCarga.asStateFlow()

    private val _errorGuardado = MutableStateFlow("")
    val errorGuardado: StateFlow<String> = _errorGuardado.asStateFlow()

    // Cargas recientes desde Firestore
    private val _cargasRecientes = MutableStateFlow<List<CargaReciente>>(emptyList())
    val cargasRecientes: StateFlow<List<CargaReciente>> = _cargasRecientes.asStateFlow()

    // Banco detectado automáticamente
    private val _bancoDetectado = MutableStateFlow("")
    val bancoDetectado: StateFlow<String> = _bancoDetectado.asStateFlow()

    init {
        cargarHistorial()
    }

    fun actualizarEmail(email: String) {
        if (email.isNotBlank()) {
            sessionManager.guardarSesion(sessionManager.obtenerNombre(), email)
            _userEmail.value = sessionManager.obtenerEmail()
            movimientoRepository.actualizarAhora()
            cargarHistorial()
        }
    }

    // Helper para obtener el nombre real del archivo desde un Uri
    fun obtenerNombreReal(uri: Uri?): String {
        if (uri == null) return "extracto.pdf"
        val context: Context = getApplication()
        var nombre = "extracto.pdf"
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (colIdx != -1) {
                            val valName = cursor.getString(colIdx)
                            if (!valName.isNullOrBlank()) nombre = valName
                        }
                    }
                }
            } else if (uri.scheme == "file") {
                nombre = uri.lastPathSegment ?: "extracto.pdf"
            }
        } catch (_: Exception) {
            nombre = uri.lastPathSegment?.substringAfterLast("/") ?: "extracto.pdf"
        }
        if (nombre.startsWith("document:")) {
            nombre = "Extracto_Bancario.pdf"
        }
        return nombre
    }

    // Carga el historial de extractos procesados desde Firestore (ordenado en memoria)
    private fun cargarHistorial() {
        val email = sessionManager.obtenerEmail()
        if (email.isBlank()) return

        viewModelScope.launch {
            try {
                val lista = extractoRepository.listar().map { extracto ->
                    CargaReciente(
                        nombreArchivo = extracto.nombreArchivo,
                        banco = extracto.banco,
                        fechaProcesado = extracto.fechaProcesado,
                        totalMovimientos = extracto.totalMovimientos,
                        estado = extracto.estado
                    )
                }

                _cargasRecientes.value = lista.sortedByDescending { it.fechaProcesado }.take(5)
            } catch (_: Exception) {
                _cargasRecientes.value = emptyList()
                _avisoCarga.value = "No se pudo consultar el historial de extractos. Revisa tu sesión y la conexión."
            }
        }
    }

    // Guarda el registro de carga en Firestore
    private suspend fun guardarRegistroCarga(nombre: String, banco: String, totalMov: Int) {
        val email = sessionManager.obtenerEmail()
        if (email.isBlank()) return

        extractoRepository.registrar(ExtractoRequest(nombre, banco, totalMov))
        cargarHistorial()
    }

    // Procesa el extracto PDF o archivo seleccionado
    fun procesarExtracto(uri: Uri?, nombreSugerido: String, contrasena: String = "") {
        val emailActual = sessionManager.obtenerEmail()
        val nombreLimpio = if (nombreSugerido.startsWith("document:")) obtenerNombreReal(uri) else nombreSugerido
        _nombreArchivo.value = nombreLimpio
        _cargando.value = true
        _guardadoExitoso.value = false
        _pdfSolicitaContrasena.value = false
        _avisoCarga.value = ""
        _errorGuardado.value = ""

        viewModelScope.launch {
            try {
                val resultado = aiService.procesarExtracto(
                    context = getApplication(),
                    uri = uri,
                    nombreArchivo = nombreLimpio,
                    userEmail = emailActual,
                    contrasena = contrasena
                )
                _extractoResultado.value = resultado
                _bancoDetectado.value = resultado.nombreBanco

                _cargando.value = false

                if (resultado.movimientosExtraidos.isNotEmpty()) {
                    // El análisis solo prepara una vista previa. Los movimientos se guardan
                    // únicamente cuando el usuario confirma con el botón.
                    viewModelScope.launch {
                        val errores = mutableListOf<String>()
                        try {
                            cuentaRepository.guardar(CuentaResponse(
                                userEmail = emailActual,
                                saldoInicial = resultado.saldoInicial,
                                saldoFinal = resultado.saldoFinal,
                                totalIngresos = resultado.totalIngresosCalculado,
                                totalGastos = resultado.totalGastosCalculado,
                                banco = resultado.nombreBanco
                            ))
                            sessionManager.guardarSaldoInicial(resultado.saldoInicial)
                        } catch (exception: Exception) {
                            errores += "No se pudo guardar el saldo de la cuenta: ${mensajeErrorServidor(exception)}"
                        }

                        try {
                            guardarRegistroCarga(nombreLimpio, resultado.nombreBanco, resultado.movimientosExtraidos.size)
                        } catch (exception: Exception) {
                            errores += "No se pudo guardar el extracto en el historial: ${mensajeErrorServidor(exception)}"
                        }
                        _avisoCarga.value = errores.joinToString("\n")
                    }
                }
            } catch (e: Exception) {
                if (e is PdfPasswordRequiredException) {
                    _pdfSolicitaContrasena.value = true
                    _extractoResultado.value = null
                    return@launch
                }
                _extractoResultado.value = ExtractoAnalizado(
                    resumenIA = "Error al procesar el extracto: ${e.localizedMessage ?: "Formato no reconocido"}"
                )
            } finally {
                _cargando.value = false
            }
        }
    }

    // Procesa texto pegado manualmente por el usuario
    fun procesarTextoDirecto(texto: String) {
        if (texto.isBlank()) return
        val emailActual = sessionManager.obtenerEmail()
        _cargando.value = true
        _guardadoExitoso.value = false
        _avisoCarga.value = ""
        _errorGuardado.value = ""

        viewModelScope.launch {
            try {
                val resultado = aiService.procesarTexto(
                    texto = texto,
                    userEmail = emailActual
                )
                _extractoResultado.value = resultado
                _bancoDetectado.value = resultado.nombreBanco
                _nombreArchivo.value = "Texto_Extracto_${resultado.nombreBanco}.txt"

                _cargando.value = false

                if (resultado.movimientosExtraidos.isNotEmpty()) {
                    val movimientosConEmail = resultado.movimientosExtraidos.map {
                        it.copy(usuarioEmail = emailActual)
                    }

                    viewModelScope.launch {
                        val errores = mutableListOf<String>()
                        try {
                            cuentaRepository.guardar(CuentaResponse(
                                userEmail = emailActual,
                                saldoInicial = resultado.saldoInicial,
                                saldoFinal = resultado.saldoFinal,
                                totalIngresos = resultado.totalIngresosCalculado,
                                totalGastos = resultado.totalGastosCalculado,
                                banco = resultado.nombreBanco
                            ))
                            sessionManager.guardarSaldoInicial(resultado.saldoInicial)
                        } catch (exception: Exception) {
                            errores += "No se pudo guardar el saldo de la cuenta: ${mensajeErrorServidor(exception)}"
                        }

                        try {
                            guardarRegistroCarga(
                                "Texto - ${resultado.nombreBanco}",
                                resultado.nombreBanco,
                                movimientosConEmail.size
                            )
                        } catch (exception: Exception) {
                            errores += "No se pudo guardar el extracto en el historial: ${mensajeErrorServidor(exception)}"
                        }
                        _avisoCarga.value = errores.joinToString("\n")
                    }
                }
            } catch (e: Exception) {
                _extractoResultado.value = ExtractoAnalizado(
                    resumenIA = "Error al analizar el texto del extracto: ${e.localizedMessage}"
                )
            } finally {
                _cargando.value = false
            }
        }
    }

    // Guardado explícito al hacer clic en el botón con redirección
    fun guardarMovimientosEnNube(onCompletado: () -> Unit) {
        if (_guardandoMovimientos.value || _guardadoExitoso.value) return
        val emailActual = sessionManager.obtenerEmail()
        val resultado = _extractoResultado.value

        if (resultado != null && resultado.movimientosExtraidos.isNotEmpty()) {
            _guardandoMovimientos.value = true
            _errorGuardado.value = ""
            val movimientosConEmail = resultado.movimientosExtraidos.map {
                it.copy(usuarioEmail = emailActual)
            }

            viewModelScope.launch {
                try {
                    movimientoRepository.insertarLista(movimientosConEmail)
                    _guardadoExitoso.value = true
                    onCompletado()
                } catch (exception: Exception) {
                    _errorGuardado.value = mensajeErrorServidor(exception)
                } finally {
                    _guardandoMovimientos.value = false
                }
            }
        } else {
            onCompletado()
        }
    }

    fun limpiar() {
        _extractoResultado.value = null
        _nombreArchivo.value = ""
        _guardadoExitoso.value = false
        _guardandoMovimientos.value = false
        _bancoDetectado.value = ""
        _pdfSolicitaContrasena.value = false
        _avisoCarga.value = ""
        _errorGuardado.value = ""
    }

    private fun mensajeErrorServidor(exception: Exception): String {
        if (exception is HttpException) {
            return when (exception.code()) {
                401 -> "Tu sesión venció. Cierra sesión e inicia sesión otra vez."
                403 -> "El servidor rechazó el acceso. Inicia sesión nuevamente."
                in 500..599 -> "El servidor no pudo guardar los datos (HTTP ${exception.code()})."
                else -> exception.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    ?: "El servidor rechazó la solicitud (HTTP ${exception.code()})."
            }
        }
        return if (exception is java.io.IOException) {
            "No se pudo conectar con el servidor. Verifica que siga activo en el puerto 8080."
        } else exception.localizedMessage ?: "Ocurrió un error inesperado."
    }
}
