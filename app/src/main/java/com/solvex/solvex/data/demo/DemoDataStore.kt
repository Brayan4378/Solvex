package com.solvex.solvex.data.demo

import com.solvex.solvex.data.entity.MovimientoEntity
import com.solvex.solvex.data.remote.CuentaResponse
import com.solvex.solvex.data.remote.ExtractoRequest
import com.solvex.solvex.data.remote.ExtractoResponse
import com.solvex.solvex.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/** Datos efímeros de presentación. Solo los usa la variante Debug. */
object DemoDataStore {
    const val EMAIL = "demo@solvex.local"
    const val NAME = "Usuario Demo"

    private val _movimientos = MutableStateFlow(sampleMovimientos())
    val movimientos = _movimientos.asStateFlow()

    private val _cuenta = MutableStateFlow(sampleCuenta())
    private val _extractos = MutableStateFlow(sampleExtractos())

    fun isDemo(email: String): Boolean = BuildConfig.DEMO_MODE && email.trim().equals(EMAIL, ignoreCase = true)

    fun iniciarSesion() {
        _movimientos.value = sampleMovimientos()
        _cuenta.value = sampleCuenta()
        _extractos.value = sampleExtractos()
    }

    fun guardar(movimiento: MovimientoEntity): MovimientoEntity {
        val id = if (movimiento.id > 0) movimiento.id else ((_movimientos.value.maxOfOrNull { it.id } ?: 0) + 1)
        val saved = movimiento.copy(id = id, usuarioEmail = EMAIL)
        _movimientos.value = if (_movimientos.value.any { it.id == id }) {
            _movimientos.value.map { if (it.id == id) saved else it }
        } else listOf(saved) + _movimientos.value
        return saved
    }

    fun eliminar(id: Int) {
        _movimientos.value = _movimientos.value.filterNot { it.id == id }
    }

    fun eliminarTodosMovimientos() {
        _movimientos.value = emptyList()
    }

    fun cuenta(): CuentaResponse = _cuenta.value

    fun guardarCuenta(cuenta: CuentaResponse): CuentaResponse {
        _cuenta.value = cuenta.copy(userEmail = EMAIL)
        return _cuenta.value
    }

    fun eliminarCuenta() {
        _cuenta.value = CuentaResponse(userEmail = EMAIL)
    }

    fun extractos(): List<ExtractoResponse> = _extractos.value

    fun registrarExtracto(request: ExtractoRequest): ExtractoResponse {
        val response = ExtractoResponse(
            id = "demo-${System.currentTimeMillis()}",
            nombreArchivo = request.nombreArchivo,
            banco = request.banco,
            fechaProcesado = System.currentTimeMillis(),
            totalMovimientos = request.totalMovimientos,
            estado = request.estado
        )
        _extractos.value = listOf(response) + _extractos.value
        return response
    }

    fun eliminarExtractos() {
        _extractos.value = emptyList()
    }

    private fun sampleMovimientos(): List<MovimientoEntity> {
        val now = System.currentTimeMillis()
        fun date(daysAgo: Int): Long = now - daysAgo * 86_400_000L
        return listOf(
            MovimientoEntity(1, EMAIL, "Nómina mensual", 4_850_000.0, "Ingresos y Rendimientos", "ingreso", date(2)),
            MovimientoEntity(2, EMAIL, "Rendimientos de ahorro", 18_450.0, "Ingresos y Rendimientos", "ingreso", date(4)),
            MovimientoEntity(3, EMAIL, "Mercado del mes", 284_600.0, "Supermercados y Mercado", "gasto", date(1)),
            MovimientoEntity(4, EMAIL, "Almuerzo", 32_500.0, "Comida y Restaurantes", "gasto", date(2)),
            MovimientoEntity(5, EMAIL, "EDS Terpel", 82_000.0, "Transporte y Gasolina", "gasto", date(3)),
            MovimientoEntity(6, EMAIL, "Internet hogar", 96_900.0, "Facturas y Servicios", "gasto", date(5)),
            MovimientoEntity(7, EMAIL, "Spotify", 19_900.0, "Entretenimiento", "gasto", date(7)),
            MovimientoEntity(8, EMAIL, "Farmacia", 43_700.0, "Salud y Droguerías", "gasto", date(8)),
            MovimientoEntity(9, EMAIL, "Compra en tienda", 126_000.0, "Compras y Comercio", "gasto", date(9)),
            MovimientoEntity(10, EMAIL, "Transferencia recibida", 120_000.0, "Transferencias y Envíos", "ingreso", date(12)),
            MovimientoEntity(11, EMAIL, "Café", 12_800.0, "Comida y Restaurantes", "gasto", date(13)),
            MovimientoEntity(12, EMAIL, "Ahorro mensual", 250_000.0, "Bolsillos y Ahorro", "gasto", date(15)),
            MovimientoEntity(13, EMAIL, "Transporte urbano", 18_600.0, "Transporte y Gasolina", "gasto", date(16)),
            MovimientoEntity(14, EMAIL, "Servicio de energía", 118_400.0, "Facturas y Servicios", "gasto", date(18))
        )
    }

    private fun sampleCuenta(): CuentaResponse {
        val gastos = sampleMovimientos().filter { it.tipo == "gasto" }.sumOf { it.monto }
        val ingresos = sampleMovimientos().filter { it.tipo == "ingreso" }.sumOf { it.monto }
        return CuentaResponse(
            userEmail = EMAIL,
            saldoInicial = 4_200_000.0,
            saldoFinal = 4_200_000.0 + ingresos - gastos,
            totalIngresos = ingresos,
            totalGastos = gastos,
            banco = "Davivienda (demo)"
        )
    }

    private fun sampleExtractos(): List<ExtractoResponse> {
        val calendar = Calendar.getInstance()
        return listOf(
            ExtractoResponse(
                id = "demo-extracto-1",
                nombreArchivo = "Extracto_Davivienda_demo.pdf",
                banco = "Davivienda",
                fechaProcesado = calendar.timeInMillis,
                totalMovimientos = 14,
                estado = "Demo"
            )
        )
    }
}
