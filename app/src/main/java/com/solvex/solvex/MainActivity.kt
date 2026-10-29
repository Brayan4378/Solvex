package com.solvex.solvex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solvex.solvex.ui.screens.LoginScreen
import com.solvex.solvex.ui.screens.MainScreen
import com.solvex.solvex.ui.screens.SignUpScreen
import com.solvex.solvex.ui.screens.SplashScreen
import com.solvex.solvex.ui.theme.SolvexTheme
import com.solvex.solvex.ui.viewmodel.AppScreen
import com.solvex.solvex.ui.viewmodel.AuthViewModel
import com.solvex.solvex.ui.viewmodel.DashboardViewModel
import com.solvex.solvex.ui.viewmodel.MovimientosViewModel
import kotlinx.coroutines.launch

// actividad principal que conecta los viewmodels con las vistas
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SolvexTheme {
                // viewmodels instanciados por compose
                val authViewModel: AuthViewModel = viewModel()
                val dashboardViewModel: DashboardViewModel = viewModel()
                val movimientosViewModel: MovimientosViewModel = viewModel()

                // observar el estado de navegacion y datos del auth viewmodel
                val currentScreen by authViewModel.currentScreen.collectAsState()
                val userName by authViewModel.userName.collectAsState()
                val userEmail by authViewModel.userEmail.collectAsState()
                val signUpErrorMsg by authViewModel.signUpErrorMsg.collectAsState()

                val scope = rememberCoroutineScope()

                if (currentScreen == AppScreen.SPLASH) {
                    // pantalla de splash que se muestra al abrir la app
                    SplashScreen(
                        modifier = Modifier.fillMaxSize(),
                        onSplashFinished = { authViewModel.onSplashFinished() }
                    )
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        when (currentScreen) {
                            AppScreen.SPLASH -> {}
                            // pantalla de login
                            AppScreen.LOGIN -> {
                                LoginScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    onLoginClick = { email, pass ->
                                        authViewModel.login(email, pass)
                                    },
                                    onGoogleLoginClick = {
                                        scope.launch {
                                            val result = authViewModel.googleAuthManager.signIn()
                                            authViewModel.procesarLoginGoogle(result)
                                        }
                                    },
                                    onNavigateToRegister = {
                                        authViewModel.navigateTo(AppScreen.SIGNUP)
                                    }
                                )
                            }
                            // pantalla de registro
                            AppScreen.SIGNUP -> {
                                SignUpScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    errorMessage = signUpErrorMsg,
                                    onSignUpClick = { fullName, email, pass ->
                                        authViewModel.signUp(fullName, email, pass)
                                    },
                                    onGoogleSignUpClick = {
                                        scope.launch {
                                            val result = authViewModel.googleAuthManager.signIn()
                                            authViewModel.procesarLoginGoogle(result)
                                        }
                                    },
                                    onNavigateToLogin = {
                                        authViewModel.navigateTo(AppScreen.LOGIN)
                                    },
                                    onBackClick = {
                                        authViewModel.navigateTo(AppScreen.LOGIN)
                                    }
                                )
                            }
                            // pantalla principal con el dashboard y las tabs de abajo
                            AppScreen.DASHBOARD -> {
                                MainScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    userName = userName,
                                    userEmail = userEmail,
                                    dashboardViewModel = dashboardViewModel,
                                    movimientosViewModel = movimientosViewModel,
                                    onLogout = { authViewModel.logout() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}