package com.solvex.solvex.data.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.solvex.solvex.data.repository.CuentaRepository
import com.solvex.solvex.session.SessionManager
import java.util.concurrent.TimeUnit

/** Refresca la copia de la cuenta que ya está guardada en Solvex cuando hay conexión. */
class ActualizacionCuentaWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val session = SessionManager(applicationContext)
        if (!session.estaLogueado() || session.obtenerToken().isBlank()) return Result.success()
        return try {
            val cuenta = CuentaRepository(applicationContext).obtener()
            session.guardarSaldoInicial(cuenta.saldoInicial)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIOD_HOURS = 4L
        private fun workName(email: String) = "solvex-account-refresh-${email.hashCode()}"

        fun programar(context: Context, email: String) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = PeriodicWorkRequestBuilder<ActualizacionCuentaWorker>(PERIOD_HOURS, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                workName(email), ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun cancelar(context: Context, email: String) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(workName(email))
        }
    }
}
