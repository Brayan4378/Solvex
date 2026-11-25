package com.solvex.solvex.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.solvex.solvex.data.entity.MovimientoEntity
import com.solvex.solvex.data.repository.MovimientoRepository
import com.solvex.solvex.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Locale

data class CategoriaBarra(
    val nombreAbr: String,
    val nombreCompleto: String,
    val monto: Double,
    val proporcion: Float,
    val esRojo: Boolean
)

data class PresupuestoCategoria(
    val categoria: String,
    val gastado: Double,
    val limite: Double,
    val porcentaje: Int
)

data class SuscripcionElemento(
    val titulo: String,
    val monto: Double,
    val iconoTipo: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalisisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MovimientoRepository(application)
    private val sessionManager = SessionManager(application)
    private val preferences = application.getSharedPreferences("solvex_profile", Context.MODE_PRIVATE)

    private val _userEmail = MutableStateFlow(sessionManager.obtenerEmail())

    // 0 = Semana, 1 = Mes, 2 = Año
    private val _periodoSeleccionado = MutableStateFlow(1)
    val periodoSeleccionado: StateFlow<Int> = _periodoSeleccionado.asStateFlow()

    val movimientos: StateFlow<List<MovimientoEntity>> = _userEmail.flatMapLatest { email ->
        repository.obtenerPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Movimientos del periodo seleccionado
    val movimientosFiltrados: StateFlow<List<MovimientoEntity>> = combine(movimientos, _periodoSeleccionado) { lista, _ ->
        if (lista.isEmpty()) return@combine emptyList()
        val (inicio, fin) = rangoPeriodo(lista)
        lista.filter { it.fecha in inicio..fin }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Datos para el gráfico de barras verticales ("Gastos por categoría")
    val barrasCategorias: StateFlow<List<CategoriaBarra>> = movimientosFiltrados.map { lista ->
        val gastos = lista.filter { it.tipo == "gasto" }
        if (gastos.isEmpty()) return@map emptyList()

        val agrupados = gastos.groupBy { it.categoria }
            .mapValues { (_, v) -> v.sumOf { it.monto } }

        val maxGasto = agrupados.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

        agrupados.entries.sortedByDescending { it.value }.take(5).mapIndexed { idx, (cat, monto) ->
            val abrev = when (cat) {
                "Comida y Restaurantes" -> "Comida"
                "Supermercados y Mercado" -> "Mercado"
                "Entretenimiento" -> "Entret."
                "Facturas y Servicios" -> "Servicios"
                "Transporte y Gasolina" -> "Transp."
                "Compras y Comercio" -> "Compras"
                "Pago de Tarjetas" -> "Tarjetas"
                "Salud y Droguerías" -> "Salud"
                "Bolsillos y Ahorro" -> "Ahorro"
                "Transferencias y Envíos" -> "Transf."
                "Comisiones e Impuestos" -> "Comis."
                else -> cat.take(7)
            }
            CategoriaBarra(
                nombreAbr = abrev,
                nombreCompleto = cat,
                monto = monto,
                proporcion = (monto / maxGasto).toFloat().coerceIn(0.15f, 1.0f),
                esRojo = idx == 1 || idx == 3
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Datos para los "Presupuestos activos"
    val presupuestosActivos: StateFlow<List<PresupuestoCategoria>> = movimientosFiltrados.map { lista ->
        val gastos = lista.filter { it.tipo == "gasto" }
        val agrupados = gastos.groupBy { it.categoria }
            .mapValues { (_, v) -> v.sumOf { it.monto } }

        if (agrupados.isEmpty()) {
            return@map listOf(
                PresupuestoCategoria("Comida", 0.0, limiteConfigurado("Comida y Restaurantes", 600000.0), 0),
                PresupuestoCategoria("Transporte", 0.0, limiteConfigurado("Transporte y Gasolina", 200000.0), 0),
                PresupuestoCategoria("Entret.", 0.0, limiteConfigurado("Entretenimiento", 150000.0), 0)
            )
        }

        agrupados.entries.sortedByDescending { it.value }.take(4).map { (cat, gastado) ->
            val limitePredeterminado = when (cat) {
                "Comida y Restaurantes" -> 600000.0
                "Supermercados y Mercado" -> 500000.0
                "Transporte y Gasolina" -> 200000.0
                "Entretenimiento" -> 150000.0
                "Facturas y Servicios" -> 300000.0
                "Compras y Comercio" -> 400000.0
                else -> (gastado * 1.25).coerceAtLeast(100000.0)
            }
            val limiteEstimado = limiteConfigurado(cat, limitePredeterminado)
            val pct = ((gastado / limiteEstimado) * 100).toInt().coerceIn(1, 100)
            val nombreMostrar = when (cat) {
                "Comida y Restaurantes" -> "Comida"
                "Supermercados y Mercado" -> "Mercado"
                "Entretenimiento" -> "Entret."
                "Transporte y Gasolina" -> "Transp."
                "Facturas y Servicios" -> "Servicios"
                "Compras y Comercio" -> "Compras"
                else -> cat.take(10)
            }
            PresupuestoCategoria(
                categoria = nombreMostrar,
                gastado = gastado,
                limite = limiteEstimado,
                porcentaje = pct
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Datos para "Gastos hormiga & Suscripciones"
    val suscripcionesYGastosHormiga: StateFlow<List<SuscripcionElemento>> = movimientos.map { lista ->
        val suscripcionesDetectadas = lista.filter { m ->
            val t = m.titulo.lowercase(Locale.ROOT)
            m.tipo == "gasto" && (
                    t.contains("netflix") || t.contains("spotify") || t.contains("icloud") ||
                            t.contains("disney") || t.contains("youtube") || t.contains("prime") ||
                            t.contains("apple") || t.contains("rappi") || t.contains("d1") ||
                            t.contains("starbucks") || t.contains("uber")
                    )
        }

        if (suscripcionesDetectadas.isEmpty()) {
            return@map listOf(
                SuscripcionElemento("Netflix Premium", 35000.0, "netflix"),
                SuscripcionElemento("Spotify Familiar", 16900.0, "spotify"),
                SuscripcionElemento("iCloud 200GB", 12900.0, "icloud")
            )
        }

        suscripcionesDetectadas.distinctBy { it.titulo }.take(5).map { m ->
            val t = m.titulo.lowercase(Locale.ROOT)
            val (nombreLimpio, tipo) = when {
                t.contains("netflix") -> Pair("Netflix Premium", "netflix")
                t.contains("spotify") -> Pair("Spotify Familiar", "spotify")
                t.contains("icloud") || t.contains("apple") -> Pair("iCloud 200GB", "icloud")
                t.contains("rappi") -> Pair("Rappi", "general")
                t.contains("uber") -> Pair("Uber Trips", "general")
                t.contains("d1") -> Pair("Tiendas D1", "general")
                t.contains("starbucks") -> Pair("Starbucks", "general")
                else -> Pair(m.titulo.take(25), "general")
            }
            SuscripcionElemento(
                titulo = nombreLimpio,
                monto = m.monto,
                iconoTipo = tipo
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun actualizarEmail(email: String) {
        _userEmail.value = email
        repository.actualizarAhora()
    }

    fun cambiarPeriodo(periodo: Int) {
        _periodoSeleccionado.value = periodo
    }

    private fun limiteConfigurado(categoria: String, predeterminado: Double): Double =
        preferences.getString("limite:${_userEmail.value}:$categoria", null)?.toDoubleOrNull() ?: predeterminado

    private fun rangoPeriodo(lista: List<MovimientoEntity>): Pair<Long, Long> {
        val refTime = if (lista.isNotEmpty()) {
            val maxFecha = lista.maxOf { it.fecha }
            val now = System.currentTimeMillis()
            if (maxFecha > now) now else maxFecha
        } else {
            System.currentTimeMillis()
        }

        val cal = Calendar.getInstance()
        cal.timeInMillis = refTime
        val fin = cal.timeInMillis

        when (_periodoSeleccionado.value) {
            0 -> { // Semana
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
            }
            1 -> { // Mes
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
            }
            2 -> { // Año
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
            }
        }
        return Pair(cal.timeInMillis, fin)
    }
}
