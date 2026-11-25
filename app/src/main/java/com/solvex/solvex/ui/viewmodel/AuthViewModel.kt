package com.solvex.solvex.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solvex.solvex.AuthResult
import com.solvex.solvex.BuildConfig
import com.solvex.solvex.GoogleAuthManager
import com.solvex.solvex.data.demo.DemoDataStore
import com.solvex.solvex.session.SessionManager
import com.solvex.solvex.data.remote.AuthRequest
import com.solvex.solvex.data.remote.GoogleRequest
import com.solvex.solvex.data.remote.RegisterRequest
import com.solvex.solvex.data.remote.SolvexApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// las pantallas que tiene la app
enum class AppScreen {
    SPLASH,
    LOGIN,
    SIGNUP,
    DASHBOARD
}

// viewmodel que maneja la autenticacion, navegacion y estado global de sesion
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val api = SolvexApiClient.get(application)
    val googleAuthManager = GoogleAuthManager(application)
    val demoModeAvailable: Boolean = BuildConfig.DEMO_MODE

    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _userName = MutableStateFlow(sessionManager.obtenerNombre())
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow(sessionManager.obtenerEmail())
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _signUpErrorMsg = MutableStateFlow("")
    val signUpErrorMsg: StateFlow<String> = _signUpErrorMsg.asStateFlow()

    private val _loginErrorMsg = MutableStateFlow("")
    val loginErrorMsg: StateFlow<String> = _loginErrorMsg.asStateFlow()

    // cuando el splash termina decidimos si ir al dashboard o al login
    fun onSplashFinished() {
        if (sessionManager.estaLogueado() && sessionManager.obtenerToken().isNotBlank()) {
            _userName.value = sessionManager.obtenerNombre()
            _userEmail.value = sessionManager.obtenerEmail()
            _currentScreen.value = AppScreen.DASHBOARD
        } else {
            // Las sesiones creadas antes de la API no tienen JWT; vuelve a autenticar para obtener uno.
            sessionManager.cerrarSesion()
            _currentScreen.value = AppScreen.LOGIN
        }
    }

    // login con correo y contrasena
    fun login(email: String, pass: String) {
        _loginErrorMsg.value = ""
        if (email.isBlank() || pass.isBlank()) {
            _loginErrorMsg.value = "Completa todos los campos"
            return
        }
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { api.login(AuthRequest(email.trim().lowercase(), pass)) }
                sessionManager.guardarSesion(response.usuario.nombre, response.usuario.email, response.accessToken)
                _userName.value = response.usuario.nombre
                _userEmail.value = response.usuario.email
                _loginErrorMsg.value = ""
                _currentScreen.value = AppScreen.DASHBOARD
            } catch (exception: Exception) {
                _loginErrorMsg.value = mensajeApi(exception, "No se pudo iniciar sesión con el servidor")
            }
        }
    }

    fun loginDemo() {
        if (!BuildConfig.DEMO_MODE) return
        DemoDataStore.iniciarSesion()
        sessionManager.guardarSesion(DemoDataStore.NAME, DemoDataStore.EMAIL, "local-demo-session")
        _userName.value = DemoDataStore.NAME
        _userEmail.value = DemoDataStore.EMAIL
        _loginErrorMsg.value = ""
        _currentScreen.value = AppScreen.DASHBOARD
    }

    // registro con nombre, correo y contrasena
    fun signUp(fullName: String, email: String, pass: String) {
        _signUpErrorMsg.value = ""
        if (fullName.isBlank() || email.isBlank() || pass.isBlank()) {
            _signUpErrorMsg.value = "Por favor completa todos los campos"
            return
        }
        if (pass.length < 8) {
            _signUpErrorMsg.value = "La contraseña debe tener mínimo 8 caracteres"
            return
        }
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    api.register(RegisterRequest(fullName.trim(), email.trim().lowercase(), pass))
                }
                sessionManager.guardarSesion(response.usuario.nombre, response.usuario.email, response.accessToken)
                _userName.value = response.usuario.nombre
                _userEmail.value = response.usuario.email
                _signUpErrorMsg.value = ""
                _currentScreen.value = AppScreen.DASHBOARD
            } catch (exception: Exception) {
                _signUpErrorMsg.value = mensajeApi(exception, "No se pudo crear la cuenta en el servidor")
            }
        }
    }

    // procesar resultado exitoso de google
    fun procesarLoginGoogle(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                val googleUser = result.user
                viewModelScope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) { api.googleLogin(GoogleRequest(googleUser.idToken)) }
                        sessionManager.guardarSesion(response.usuario.nombre, response.usuario.email, response.accessToken)
                        _userName.value = response.usuario.nombre
                        _userEmail.value = response.usuario.email
                        _signUpErrorMsg.value = ""
                        _loginErrorMsg.value = ""
                        _currentScreen.value = AppScreen.DASHBOARD
                    } catch (exception: Exception) {
                        _loginErrorMsg.value = mensajeApi(exception, "No se pudo validar la cuenta de Google")
                    }
                }
            }
            is AuthResult.Cancelled -> {
                // usuario cancelo la seleccion
            }
            is AuthResult.Error -> {
                _loginErrorMsg.value = result.message
            }
        }
    }

    // cerrar sesion
    fun logout() {
        viewModelScope.launch {
            googleAuthManager.signOut()
            sessionManager.cerrarSesion()
            _userName.value = ""
            _userEmail.value = ""
            _currentScreen.value = AppScreen.LOGIN
        }
    }

    // navegacion entre pantallas
    fun navigateTo(screen: AppScreen) {
        _signUpErrorMsg.value = ""
        _loginErrorMsg.value = ""
        _currentScreen.value = screen
    }

    private fun mensajeApi(exception: Exception, fallback: String): String {
        var cause: Throwable? = exception
        while (cause != null) {
            if (cause is ConnectException || cause is UnknownHostException || cause is SocketTimeoutException) {
                return "No hay conexión con el backend. Inícialo en el puerto 8080. En emulador usa 10.0.2.2; en un celular, configura la IP local de tu computador."
            }
            cause = cause.cause
        }
        val body = (exception as? HttpException)?.response()?.errorBody()?.string()
        val message = body?.let { runCatching { JSONObject(it).optString("error") }.getOrNull() }
        return message?.takeIf { it.isNotBlank() } ?: fallback
    }
}
