package com.solvex.solvex.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solvex.solvex.data.entity.MovimientoEntity
import com.solvex.solvex.data.repository.FirestoreMovimientoRepository
import com.solvex.solvex.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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

    private val repository = FirestoreMovimientoRepository()
    private val sessionManager = SessionManager(application)

    private val _userEmail = MutableStateFlow(sessionManager.obtenerEmail())

    val movimientos: StateFlow<List<MovimientoEntity>> = _userEmail.flatMapLatest { email ->
        repository.obtenerPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _mensajes = MutableStateFlow<List<MensajeChat>>(
        listOf(
            MensajeChat(
                texto = "Hola. Soy tu asistente de Solvex AI. 🟢\n\nPuedes preguntarme sobre tus gastos reales en comida, transporte, suscripciones o pedirme un análisis de tu balance.",
                esUsuario = false
            )
        )
    )
    val mensajes: StateFlow<List<MensajeChat>> = _mensajes.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    fun actualizarEmail(email: String) {
        _userEmail.value = email
    }

    fun enviarMensaje(texto: String, userName: String = "Carlos") {
        val consulta = texto.trim()
        if (consulta.isBlank()) return

        val mensajeUsuario = MensajeChat(texto = consulta, esUsuario = true)
        _mensajes.value = _mensajes.value + mensajeUsuario
        _cargando.value = true

        viewModelScope.launch {
            kotlinx.coroutines.delay(350)
            val respuesta = generarRespuestaIA(consulta, movimientos.value, userName)
            _mensajes.value = _mensajes.value + MensajeChat(texto = respuesta, esUsuario = false)
            _cargando.value = false
        }
    }

    private fun cop(monto: Double): String =
        "$" + String.format(Locale.US, "%,.0f", monto).replace(',', '.')

    private fun generarRespuestaIA(pregunta: String, movimientos: List<MovimientoEntity>, userName: String): String {
        val p = pregunta.lowercase(Locale.ROOT)
        val nombre = if (userName.isNotBlank()) userName else "Carlos"

        if (movimientos.isEmpty()) {
            return "Hola $nombre. Aún no tienes movimientos registrados en tu cuenta.\n\nSube un extracto bancario en PDF o registra tus movimientos para poder darte información real sobre tus gastos."
        }

        val refTime = movimientos.maxOfOrNull { it.fecha } ?: System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = refTime

        // Rango mes actual
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val inicioMesActual = cal.timeInMillis

        // Rango mes anterior
        cal.add(Calendar.MONTH, -1)
        val inicioMesAnterior = cal.timeInMillis
        val finMesAnterior = inicioMesActual - 1

        val movsMesActual = movimientos.filter { it.fecha >= inicioMesActual }
        val movsMesAnterior = movimientos.filter { it.fecha in inicioMesAnterior..finMesAnterior }

        val gastosMesActual = movsMesActual.filter { it.tipo == "gasto" }
        val gastosMesAnterior = movsMesAnterior.filter { it.tipo == "gasto" }

        val totalGastosActual = gastosMesActual.sumOf { it.monto }
        val totalIngresosActual = movsMesActual.filter { it.tipo == "ingreso" }.sumOf { it.monto }

        // Búsqueda por categoría específica: Comida, Transporte, Entretenimiento, Servicios, Compras, etc.
        val categoriaBuscada = when {
            p.contains("comida") || p.contains("almuerzo") || p.contains("restaurante") -> "Comida y Restaurantes"
            p.contains("mercado") || p.contains("supermercado") || p.contains("tienda") -> "Supermercados y Mercado"
            p.contains("transporte") || p.contains("uber") || p.contains("gasolina") -> "Transporte y Gasolina"
            p.contains("entretenimiento") || p.contains("cine") || p.contains("juegos") -> "Entretenimiento"
            p.contains("servicio") || p.contains("servicios") || p.contains("luz") || p.contains("agua") -> "Facturas y Servicios"
            p.contains("compras") || p.contains("ropa") || p.contains("amazon") -> "Compras y Comercio"
            p.contains("salud") || p.contains("droguería") || p.contains("farmacia") -> "Salud y Droguerías"
            p.contains("tarjeta") || p.contains("cuota de manejo") -> "Pago de Tarjetas"
            p.contains("transferencia") || p.contains("nequi") || p.contains("daviplata") -> "Transferencias y Envíos"
            p.contains("bolsillo") || p.contains("ahorro programado") -> "Bolsillos y Ahorro"
            p.contains("comisión") || p.contains("comision") || p.contains("4x1000") || p.contains("gmf") -> "Comisiones e Impuestos"
            else -> null
        }

        if (categoriaBuscada != null) {
            val gastoCatActual = gastosMesActual.filter { it.categoria == categoriaBuscada }.sumOf { it.monto }
            val gastoCatAnterior = gastosMesAnterior.filter { it.categoria == categoriaBuscada }.sumOf { it.monto }

            val nombreMostrarCat = categoriaBuscada

            if (gastoCatActual == 0.0) {
                return "Hola $nombre. Este mes no tienes gastos registrados en la categoría de $nombreMostrarCat."
            }

            val variacion = if (gastoCatAnterior > 0) {
                val diff = ((gastoCatActual - gastoCatAnterior) / gastoCatAnterior) * 100
                if (diff < 0) {
                    "\n\nEsto representa un ahorro del ${String.format(Locale.US, "%.0f", abs(diff))}% en comparación con el mismo periodo del mes pasado."
                } else if (diff > 0) {
                    "\n\nEsto representa un incremento del ${String.format(Locale.US, "%.0f", diff)}% respecto al mes pasado."
                } else ""
            } else ""

            return "Hola $nombre. Este mes has gastado un total de ${cop(gastoCatActual)} en la categoría de $nombreMostrarCat.$variacion"
        }

        // ¿En qué mes o categoría gasté más?
        if (p.contains("en qué gasté más") || p.contains("en que gaste mas") ||
            p.contains("mayor gasto") || p.contains("categoría principal") || p.contains("categoria principal")) {
            val topCat = gastosMesActual
                .groupBy { it.categoria }
                .mapValues { (_, v) -> v.sumOf { it.monto } }
                .maxByOrNull { it.value }

            if (topCat != null) {
                val pct = if (totalGastosActual > 0) (topCat.value / totalGastosActual * 100) else 0.0
                return "Hola $nombre. Tu categoría de mayor gasto este mes es ${topCat.key} con un total de ${cop(topCat.value)} (${String.format(Locale.US, "%.0f", pct)}% de tus gastos)."
            }
        }

        // Suscripciones
        if (p.contains("suscripción") || p.contains("suscripciones") || p.contains("netflix") || p.contains("spotify")) {
            val subs = movimientos.filter { m ->
                val t = m.titulo.lowercase(Locale.ROOT)
                t.contains("netflix") || t.contains("spotify") || t.contains("icloud") ||
                        t.contains("disney") || t.contains("hbo") || t.contains("youtube") || t.contains("prime")
            }
            if (subs.isEmpty()) {
                return "Hola $nombre. No detecté suscripciones registradas en tus movimientos recintes."
            }
            val totalSubs = subs.sumOf { it.monto }
            val detalle = subs.take(4).joinToString("\n") { "• ${it.titulo}: ${cop(it.monto)}" }
            return "Hola $nombre. Tus suscripciones detectadas suman ${cop(totalSubs)} al mes:\n\n$detalle"
        }

        // Totales generales de gasto
        if (p.contains("cuánto gasté") || p.contains("cuanto gaste") || p.contains("total de gastos") || p.contains("mis gastos")) {
            return "Hola $nombre. En este mes has gastado un total de ${cop(totalGastosActual)} en ${gastosMesActual.size} movimientos registrados."
        }

        // Balance y Ahorro
        if (p.contains("ahorro") || p.contains("balance") || p.contains("sobra")) {
            val balance = totalIngresosActual - totalGastosActual
            val pctAhorro = if (totalIngresosActual > 0) (balance / totalIngresosActual * 100) else 0.0
            return if (balance >= 0) {
                "Hola $nombre. Tu balance actual es positivo con ${cop(balance)}. Tienes una tasa de ahorro del ${String.format(Locale.US, "%.0f", pctAhorro)}%."
            } else {
                "Hola $nombre. Alerta: Tus gastos superan tus ingresos este mes en ${cop(abs(balance))}."
            }
        }

        // Respuesta genérica adaptada al usuario
        return "Hola $nombre. Puedo responder sobre tus gastos reales en comida, transporte, suscripciones o darte el balance de tu cuenta."
    }
}
