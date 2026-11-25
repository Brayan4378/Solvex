package com.solvex.solvex.data.repository

import android.content.Context
import com.solvex.solvex.data.demo.DemoDataStore
import com.solvex.solvex.data.remote.CuentaRequest
import com.solvex.solvex.data.remote.CuentaResponse
import com.solvex.solvex.data.remote.SolvexApiClient
import com.solvex.solvex.session.SessionManager

class CuentaRepository(context: Context) {
    private val api = SolvexApiClient.get(context.applicationContext)
    private val sessionManager = SessionManager(context.applicationContext)

    suspend fun obtener(): CuentaResponse = if (DemoDataStore.isDemo(sessionManager.obtenerEmail())) {
        DemoDataStore.cuenta()
    } else api.getCuenta()

    suspend fun guardar(cuenta: CuentaResponse): CuentaResponse = if (DemoDataStore.isDemo(cuenta.userEmail)) {
        DemoDataStore.guardarCuenta(cuenta)
    } else api.saveCuenta(
        CuentaRequest(cuenta.saldoInicial, cuenta.saldoFinal, cuenta.totalIngresos, cuenta.totalGastos, cuenta.banco)
    )

    suspend fun eliminar() {
        if (DemoDataStore.isDemo(sessionManager.obtenerEmail())) {
            DemoDataStore.eliminarCuenta()
            return
        }
        val response = api.deleteCuenta()
        if (!response.isSuccessful) error("No se pudo desvincular la cuenta")
    }
}
