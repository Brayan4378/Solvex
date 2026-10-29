package com.solvex.solvex.ui.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solvex.solvex.AuthResult
import com.solvex.solvex.GoogleAuthManager
import com.solvex.solvex.data.AppDatabase
import com.solvex.solvex.data.SessionManager
import com.solvex.solvex.data.UsuarioEntity
import com.solvex.solvex.data.repository.UsuarioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val repository: UsuarioRepository
    val googleAuthManager = GoogleAuthManager(application)

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

    init {
        val db = AppDatabase.obtenerInstancia(application)
        repository = UsuarioRepository(db.usuarioDao())
    }

    // cuando el splash termina decidimos si ir al dashboard o al login
    fun onSplashFinished() {
        if (sessionManager.estaLogueado()) {
            _userName.value = sessionManager.obtenerNombre()
            _userEmail.value = sessionManager.obtenerEmail()
            _currentScreen.value = AppScreen.DASHBOARD
        } else {
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
            val usuario = withContext(Dispatchers.IO) {
                repository.login(email.trim().lowercase(), pass)
            }
            if (usuario != null) {
                sessionManager.guardarSesion(usuario.nombre, usuario.email)
                _userName.value = usuario.nombre
                _userEmail.value = usuario.email
                _loginErrorMsg.value = ""
                _currentScreen.value = AppScreen.DASHBOARD
            } else {
                _loginErrorMsg.value = "Correo o contraseña incorrectos"
            }
        }
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
            val existe = withContext(Dispatchers.IO) {
                repository.existeEmail(email.trim().lowercase())
            }
            if (existe > 0) {
                _signUpErrorMsg.value = "El correo electrónico '$email' ya está en uso"
            } else {
                withContext(Dispatchers.IO) {
                    repository.insertar(
                        UsuarioEntity(
                            nombre = fullName.trim(),
                            email = email.trim().lowercase(),
                            contrasena = pass
                        )
                    )
                }
                sessionManager.guardarSesion(fullName.trim(), email.trim().lowercase())
                _userName.value = fullName.trim()
                _userEmail.value = email.trim().lowercase()
                _signUpErrorMsg.value = ""
                _currentScreen.value = AppScreen.DASHBOARD
            }
        }
    }

    // procesar resultado exitoso de google
    fun procesarLoginGoogle(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                val googleUser = result.user
                viewModelScope.launch {
                    val nombre = googleUser.name?.ifBlank { null }
                        ?: googleUser.email.substringBefore("@").replaceFirstChar { it.uppercase() }

                    val usuarioExistente = withContext(Dispatchers.IO) {
                        repository.buscarPorEmail(googleUser.email)
                    }
                    if (usuarioExistente == null) {
                        withContext(Dispatchers.IO) {
                            repository.insertar(
                                UsuarioEntity(
                                    nombre = nombre,
                                    email = googleUser.email,
                                    contrasena = "google_auth"
                                )
                            )
                        }
                    }

                    sessionManager.guardarSesion(nombre, googleUser.email)
                    _userName.value = nombre
                    _userEmail.value = googleUser.email
                    _signUpErrorMsg.value = ""
                    _loginErrorMsg.value = ""
                    _currentScreen.value = AppScreen.DASHBOARD
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
}
