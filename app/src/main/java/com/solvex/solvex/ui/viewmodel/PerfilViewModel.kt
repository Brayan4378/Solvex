package com.solvex.solvex.ui.viewmodel

import android.app.Application
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solvex.solvex.data.remote.CuentaResponse
import com.solvex.solvex.data.repository.CuentaRepository
import com.solvex.solvex.data.repository.ExtractoRepository
import com.solvex.solvex.data.repository.MovimientoRepository
import com.solvex.solvex.data.service.ActualizacionCuentaWorker
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
import kotlinx.coroutines.flow.collect
import java.util.Calendar

data class EntidadBancaria(
    val nombre: String,
    val tipoCuenta: String,
    val saldo: Double,
    val icono: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val movimientoRepository = MovimientoRepository(application)
    private val extractoRepository = ExtractoRepository(application)
    private val cuentaRepository = CuentaRepository(application)
    private val preferences = application.getSharedPreferences("solvex_profile", Context.MODE_PRIVATE)
    private val _userEmail = MutableStateFlow(sessionManager.obtenerEmail())

    private val _notificacionesActivas = MutableStateFlow(true)
    val notificacionesActivas: StateFlow<Boolean> = _notificacionesActivas.asStateFlow()
    private val _actualizacionAutoActiva = MutableStateFlow(false)
    val actualizacionAutoActiva: StateFlow<Boolean> = _actualizacionAutoActiva.asStateFlow()
    private val _biometriaActiva = MutableStateFlow(false)
    val biometriaActiva: StateFlow<Boolean> = _biometriaActiva.asStateFlow()
    private val _cuenta = MutableStateFlow<CuentaResponse?>(null)
    val cuenta: StateFlow<CuentaResponse?> = _cuenta.asStateFlow()
    private val _limites = MutableStateFlow<Map<String, Double>>(emptyMap())
    val limites: StateFlow<Map<String, Double>> = _limites.asStateFlow()
    private val _operacionEnCurso = MutableStateFlow(false)
    val operacionEnCurso: StateFlow<Boolean> = _operacionEnCurso.asStateFlow()
    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error.asStateFlow()

    val movimientos = _userEmail.flatMapLatest { email ->
        movimientoRepository.obtenerPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ingresosAnioActual = movimientos.map { rows ->
        val year = Calendar.getInstance().get(Calendar.YEAR)
        rows.filter { movimiento ->
            movimiento.tipo == "ingreso" && Calendar.getInstance().apply {
                timeInMillis = movimiento.fecha
            }.get(Calendar.YEAR) == year
        }.sumOf { it.monto }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val porcentajeTopeRenta = ingresosAnioActual.map { ingresos ->
        ((ingresos / TOPE_INGRESOS_RENTA_2026) * 100).toInt().coerceAtLeast(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val entidadesVinculadas: StateFlow<List<EntidadBancaria>> = _cuenta.map { cuenta ->
        if (cuenta == null || cuenta.banco.isBlank()) emptyList()
        else listOf(
            EntidadBancaria(
                nombre = cuenta.banco,
                tipoCuenta = "Cuenta registrada desde extracto",
                saldo = cuenta.saldoFinal,
                icono = iconoBanco(cuenta.banco)
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        actualizarEmail(_userEmail.value)
        viewModelScope.launch {
            var previouslySeen: Set<Int>? = null
            movimientos.collect { rows ->
                revisarUmbralRenta(rows)
                val currentIds = rows.map { it.id }.filter { it > 0 }.toSet()
                val previous = previouslySeen
                if (previous != null && _notificacionesActivas.value) {
                    rows.filter { it.id in currentIds - previous }.forEach(::notificarMovimiento)
                }
                previouslySeen = currentIds
            }
        }
    }

    fun actualizarEmail(email: String) {
        val normalized = email.trim().lowercase()
        if (normalized.isBlank()) return
        _userEmail.value = normalized
        movimientoRepository.actualizarAhora()
        cargarPreferencias(normalized)
        viewModelScope.launch {
            _cuenta.value = runCatching { cuentaRepository.obtener() }.getOrNull()
        }
    }

    fun toggleNotificaciones(activo: Boolean) {
        _notificacionesActivas.value = activo
        preferences.edit().putBoolean(key("notificaciones"), activo).apply()
    }

    fun toggleActualizacionAuto(activo: Boolean) {
        _actualizacionAutoActiva.value = activo
        preferences.edit().putBoolean(key("actualizacion_auto"), activo).apply()
        if (activo) ActualizacionCuentaWorker.programar(getApplication(), _userEmail.value)
        else ActualizacionCuentaWorker.cancelar(getApplication(), _userEmail.value)
    }

    fun toggleBiometria(activo: Boolean) {
        _biometriaActiva.value = activo
        preferences.edit().putBoolean(key("biometria"), activo).apply()
    }

    fun guardarLimites(limites: Map<String, Double>) {
        val editor = preferences.edit()
        limites.forEach { (categoria, monto) -> editor.putString(limitKey(categoria), monto.toLong().toString()) }
        editor.apply()
        _limites.value = limites
    }

    fun desvincularTodasLasCuentas(onCompletado: () -> Unit) {
        if (_operacionEnCurso.value) return
        _error.value = ""
        _operacionEnCurso.value = true
        viewModelScope.launch {
            try {
                movimientoRepository.eliminarTodosPorUsuario(_userEmail.value)
                extractoRepository.eliminarTodos()
                cuentaRepository.eliminar()
                ActualizacionCuentaWorker.cancelar(getApplication(), _userEmail.value)
                _cuenta.value = null
                onCompletado()
            } catch (exception: Exception) {
                _error.value = exception.localizedMessage ?: "No se pudieron desvincular las cuentas. Intenta de nuevo."
            } finally {
                _operacionEnCurso.value = false
            }
        }
    }

    private fun cargarPreferencias(email: String) {
        _notificacionesActivas.value = preferences.getBoolean("notificaciones:$email", false)
        _actualizacionAutoActiva.value = preferences.getBoolean("actualizacion_auto:$email", false)
        if (_actualizacionAutoActiva.value) ActualizacionCuentaWorker.programar(getApplication(), email)
        else ActualizacionCuentaWorker.cancelar(getApplication(), email)
        _biometriaActiva.value = preferences.getBoolean("biometria:$email", false)
        _limites.value = CATEGORIAS_PRESUPUESTO.associateWith { categoria ->
            preferences.getString("limite:$email:$categoria", null)?.toDoubleOrNull() ?: limitePredeterminado(categoria)
        }
    }

    private fun notificarMovimiento(movimiento: com.solvex.solvex.data.entity.MovimientoEntity) {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel("movimientos") == null) {
            manager.createNotificationChannel(
                NotificationChannel("movimientos", "Movimientos", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val tipo = if (movimiento.tipo == "ingreso") "Ingreso" else "Nuevo movimiento"
        val notification = NotificationCompat.Builder(context, "movimientos")
            .setSmallIcon(com.solvex.solvex.R.drawable.ic_launcher_foreground)
            .setContentTitle(tipo)
            .setContentText("${movimiento.titulo}: $${movimiento.monto.toLong()}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(movimiento.id, notification) }
    }

    private fun revisarUmbralRenta(rows: List<com.solvex.solvex.data.entity.MovimientoEntity>) {
        val context = getApplication<Application>()
        if (!_notificacionesActivas.value) return
        if (Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val year = Calendar.getInstance().get(Calendar.YEAR)
        val ingresos = rows.filter { movimiento ->
            movimiento.tipo == "ingreso" && Calendar.getInstance().apply {
                timeInMillis = movimiento.fecha
            }.get(Calendar.YEAR) == year
        }.sumOf { it.monto }
        val porcentaje = ingresos / TOPE_INGRESOS_RENTA_2026 * 100
        val prefsKey = "renta_notificada:${_userEmail.value}:$year"
        val preferences = context.getSharedPreferences("solvex_profile", Context.MODE_PRIVATE)
        val ultimoAviso = preferences.getInt(prefsKey, 0)
        val alcanzados = UMBRALES_RENTA.filter { it > ultimoAviso && porcentaje >= it }
        if (alcanzados.isEmpty()) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel("renta") == null) {
            manager.createNotificationChannel(
                NotificationChannel("renta", "Seguimiento de ingresos", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val notificationManager = NotificationManagerCompat.from(context)
        alcanzados.forEach { umbral ->
            val montoUmbral = TOPE_INGRESOS_RENTA_2026 * umbral / 100
            val notification = NotificationCompat.Builder(context, "renta")
                .setSmallIcon(com.solvex.solvex.R.drawable.ic_launcher_foreground)
                .setContentTitle("Ingresos al ${umbral}% del umbral de renta")
                .setContentText("Tus ingresos registrados en $year alcanzaron $${montoUmbral.toLong()}.")
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "Tus ingresos registrados en $year alcanzaron el ${umbral}% del umbral de referencia de " +
                        "${formatearPesos(TOPE_INGRESOS_RENTA_2026)}. Es un seguimiento informativo; la obligación puede depender de otros criterios DIAN."
                ))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            runCatching { notificationManager.notify(RENTA_NOTIFICATION_BASE + umbral, notification) }
        }
        preferences.edit().putInt(prefsKey, alcanzados.maxOrNull() ?: ultimoAviso).apply()
    }

    private fun formatearPesos(valor: Double): String =
        java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO")).format(valor)

    private fun key(setting: String) = "$setting:${_userEmail.value}"
    private fun limitKey(category: String) = "limite:${_userEmail.value}:$category"

    companion object {
        const val UVT_2026 = 52_374.0
        const val UVT_UMBRAL_INGRESOS_RENTA = 1_400
        const val TOPE_INGRESOS_RENTA_2026 = UVT_2026 * UVT_UMBRAL_INGRESOS_RENTA
        private const val RENTA_NOTIFICATION_BASE = 52_000
        private val UMBRALES_RENTA = listOf(50, 80, 100)

        val CATEGORIAS_PRESUPUESTO = listOf(
            "Comida y Restaurantes", "Supermercados y Mercado", "Transporte y Gasolina",
            "Entretenimiento", "Facturas y Servicios", "Compras y Comercio", "Salud y Droguerías"
        )

        fun limitePredeterminado(categoria: String): Double = when (categoria) {
            "Comida y Restaurantes" -> 600_000.0
            "Supermercados y Mercado" -> 500_000.0
            "Transporte y Gasolina" -> 200_000.0
            "Entretenimiento" -> 150_000.0
            "Facturas y Servicios" -> 300_000.0
            "Compras y Comercio" -> 400_000.0
            else -> 250_000.0
        }

        private fun iconoBanco(nombre: String): String {
            val banco = nombre.lowercase()
            return when {
                "bancolombia" in banco -> "bancolombia"
                "davivienda" in banco -> "davivienda"
                "nequi" in banco -> "nequi"
                else -> "banco"
            }
        }
    }
}
