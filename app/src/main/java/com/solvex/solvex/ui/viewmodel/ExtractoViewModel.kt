package com.solvex.solvex.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.solvex.solvex.data.entity.ExtractoAnalizado
import com.solvex.solvex.data.repository.FirestoreMovimientoRepository
import com.solvex.solvex.data.service.ExtractoAIService
import com.solvex.solvex.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    private val firestoreRepository = FirestoreMovimientoRepository()
    private val aiService = ExtractoAIService()
    private val db = FirebaseFirestore.getInstance()

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
                val snapshot = db.collection("extractos")
                    .whereEqualTo("userEmail", email)
                    .get()
                    .await()

                val lista = snapshot.documents.mapNotNull { doc ->
                    CargaReciente(
                        nombreArchivo = doc.getString("nombreArchivo") ?: "",
                        banco = doc.getString("banco") ?: "",
                        fechaProcesado = doc.getLong("fechaProcesado") ?: 0L,
                        totalMovimientos = doc.getLong("totalMovimientos")?.toInt() ?: 0,
                        estado = doc.getString("estado") ?: "Procesado"
                    )
                }

                _cargasRecientes.value = lista.sortedByDescending { it.fechaProcesado }.take(5)
            } catch (_: Exception) {
                _cargasRecientes.value = emptyList()
            }
        }
    }

    // Guarda el registro de carga en Firestore
    private suspend fun guardarRegistroCarga(nombre: String, banco: String, totalMov: Int) {
        val email = sessionManager.obtenerEmail()
        if (email.isBlank()) return

        try {
            val docRef = db.collection("extractos").document()
            val datos = hashMapOf(
                "userEmail" to email,
                "nombreArchivo" to nombre,
                "banco" to banco,
                "fechaProcesado" to System.currentTimeMillis(),
                "totalMovimientos" to totalMov,
                "estado" to "Procesado"
            )
            docRef.set(datos).await()
            cargarHistorial()
        } catch (_: Exception) {}
    }

    // Procesa el extracto PDF o archivo seleccionado
    fun procesarExtracto(uri: Uri?, nombreSugerido: String, contrasena: String = "") {
        val emailActual = sessionManager.obtenerEmail()
        val nombreLimpio = if (nombreSugerido.startsWith("document:")) obtenerNombreReal(uri) else nombreSugerido
        _nombreArchivo.value = nombreLimpio
        _cargando.value = true
        _guardadoExitoso.value = false

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
                    val movimientosConEmail = resultado.movimientosExtraidos.map {
                        it.copy(usuarioEmail = emailActual)
                    }

                    // 1. Guardar de inmediato en la memoria/repositorio (< 1ms)
                    firestoreRepository.insertarLista(movimientosConEmail)
                    _guardadoExitoso.value = true

                    // 2. Guardar en segundo plano la cuenta y el historial en Firestore
                    viewModelScope.launch {
                        try {
                            db.collection("cuentas").document(emailActual).set(
                                hashMapOf(
                                    "userEmail" to emailActual,
                                    "saldoInicial" to resultado.saldoInicial,
                                    "saldoFinal" to resultado.saldoFinal,
                                    "totalIngresos" to resultado.totalIngresosCalculado,
                                    "totalGastos" to resultado.totalGastosCalculado,
                                    "banco" to resultado.nombreBanco
                                )
                            ).await()
                            sessionManager.guardarSaldoInicial(resultado.saldoInicial)
                        } catch (_: Exception) {}

                        try {
                            guardarRegistroCarga(nombreLimpio, resultado.nombreBanco, movimientosConEmail.size)
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
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

                    firestoreRepository.insertarLista(movimientosConEmail)
                    _guardadoExitoso.value = true

                    viewModelScope.launch {
                        try {
                            db.collection("cuentas").document(emailActual).set(
                                hashMapOf(
                                    "userEmail" to emailActual,
                                    "saldoInicial" to resultado.saldoInicial,
                                    "saldoFinal" to resultado.saldoFinal,
                                    "totalIngresos" to resultado.totalIngresosCalculado,
                                    "totalGastos" to resultado.totalGastosCalculado,
                                    "banco" to resultado.nombreBanco
                                )
                            ).await()
                            sessionManager.guardarSaldoInicial(resultado.saldoInicial)
                        } catch (_: Exception) {}

                        try {
                            guardarRegistroCarga(
                                "Texto - ${resultado.nombreBanco}",
                                resultado.nombreBanco,
                                movimientosConEmail.size
                            )
                        } catch (_: Exception) {}
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
        val emailActual = sessionManager.obtenerEmail()
        val resultado = _extractoResultado.value

        if (resultado != null && resultado.movimientosExtraidos.isNotEmpty()) {
            val movimientosConEmail = resultado.movimientosExtraidos.map {
                it.copy(usuarioEmail = emailActual)
            }

            viewModelScope.launch {
                firestoreRepository.insertarLista(movimientosConEmail)
                _guardadoExitoso.value = true
                onCompletado()
            }
        } else {
            onCompletado()
        }
    }

    fun limpiar() {
        _extractoResultado.value = null
        _nombreArchivo.value = ""
        _guardadoExitoso.value = false
        _bancoDetectado.value = ""
    }
}
