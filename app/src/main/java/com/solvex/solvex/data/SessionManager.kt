package com.solvex.solvex.data

import android.content.Context
import android.content.SharedPreferences

// maneja la sesion del usuario con sharedpreferences
class SessionManager(context: Context) {
    // archivo donde se guarda la sesion
    private val prefs: SharedPreferences = context.getSharedPreferences("solvex_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ESTA_LOGUEADO = "esta_logueado"
        private const val KEY_NOMBRE = "nombre_usuario"
        private const val KEY_EMAIL = "email_usuario"
    }

    // guardar la sesion cuando el usuario inicia sesion
    fun guardarSesion(nombre: String, email: String) {
        prefs.edit().apply {
            putBoolean(KEY_ESTA_LOGUEADO, true)
            putString(KEY_NOMBRE, nombre)
            putString(KEY_EMAIL, email)
            apply()
        }
    }

    // revisar si hay una sesion activa
    fun estaLogueado(): Boolean {
        return prefs.getBoolean(KEY_ESTA_LOGUEADO, false)
    }

    // obtener el nombre del usuario guardado
    fun obtenerNombre(): String {
        return prefs.getString(KEY_NOMBRE, "") ?: ""
    }

    // obtener el email del usuario guardado
    fun obtenerEmail(): String {
        return prefs.getString(KEY_EMAIL, "") ?: ""
    }

    // cerrar la sesion y borrar los datos guardados
    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}
