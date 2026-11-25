package com.solvex.solvex.data.repository

import android.content.Context
import com.solvex.solvex.data.demo.DemoDataStore
import com.solvex.solvex.data.remote.ExtractoRequest
import com.solvex.solvex.data.remote.ExtractoResponse
import com.solvex.solvex.data.remote.SolvexApiClient
import com.solvex.solvex.session.SessionManager

class ExtractoRepository(context: Context) {
    private val api = SolvexApiClient.get(context.applicationContext)
    private val sessionManager = SessionManager(context.applicationContext)

    suspend fun listar(): List<ExtractoResponse> = if (DemoDataStore.isDemo(sessionManager.obtenerEmail())) {
        DemoDataStore.extractos()
    } else api.getExtractos()

    suspend fun registrar(request: ExtractoRequest): ExtractoResponse = if (DemoDataStore.isDemo(sessionManager.obtenerEmail())) {
        DemoDataStore.registrarExtracto(request)
    } else api.addExtracto(request)

    suspend fun eliminarTodos() {
        if (DemoDataStore.isDemo(sessionManager.obtenerEmail())) {
            DemoDataStore.eliminarExtractos()
            return
        }
        val response = api.deleteExtractos()
        if (!response.isSuccessful) error("No se pudo eliminar el historial de extractos")
    }
}
