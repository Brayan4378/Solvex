package com.solvex.solvex.session

import android.content.Context
import android.content.SharedPreferences

// Maneja la sesión del usuario con SharedPreferences
class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("solvex_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ESTA_LOGUEADO = "esta_logueado"
        private const val KEY_NOMBRE = "nombre_usuario"
        private const val KEY_EMAIL = "email_usuario"
        private const val KEY_SALDO_INICIAL = "saldo_inicial_cuenta"
    }

    // Guardar la sesión cuando el usuario inicia sesión
    fun guardarSesion(nombre: String, email: String) {
        val emailLimpio = if (email.isBlank()) "usuario@solvex.com" else email.trim().lowercase()
        val nombreLimpio = if (nombre.isBlank()) "Brayan Ocampo" else nombre.trim()
        prefs.edit().apply {
            putBoolean(KEY_ESTA_LOGUEADO, true)
            putString(KEY_NOMBRE, nombreLimpio)
            putString(KEY_EMAIL, emailLimpio)
            apply()
        }
    }

    fun guardarSaldoInicial(saldo: Double) {
        prefs.edit().putFloat(KEY_SALDO_INICIAL, saldo.toFloat()).apply()
    }

    fun obtenerSaldoInicial(): Double {
        return prefs.getFloat(KEY_SALDO_INICIAL, 0f).toDouble()
    }

    // Revisar si hay una sesión activa
    fun estaLogueado(): Boolean {
        return prefs.getBoolean(KEY_ESTA_LOGUEADO, false)
    }

    // Obtener el nombre del usuario guardado
    fun obtenerNombre(): String {
        val n = prefs.getString(KEY_NOMBRE, "") ?: ""
        return if (n.isBlank()) "Brayan Ocampo" else n
    }

    // Obtener el email del usuario guardado (Garantiza nunca retornar vacío)
    fun obtenerEmail(): String {
        val e = prefs.getString(KEY_EMAIL, "") ?: ""
        return if (e.isBlank()) "usuario@solvex.com" else e
    }

    // Cerrar la sesión y borrar los datos guardados
    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}
