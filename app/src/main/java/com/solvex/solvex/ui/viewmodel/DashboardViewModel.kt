package com.solvex.solvex.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.solvex.solvex.data.entity.MovimientoEntity
import com.solvex.solvex.data.repository.FirestoreMovimientoRepository
import com.solvex.solvex.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirestoreMovimientoRepository()
    private val sessionManager = SessionManager(application)
    private val db = FirebaseFirestore.getInstance()

    private val _userEmail = MutableStateFlow(sessionManager.obtenerEmail())

    private val _showLogoutDialog = MutableStateFlow(false)
    val showLogoutDialog: StateFlow<Boolean> = _showLogoutDialog.asStateFlow()

    // Datos reactivos del dashboard desde Firestore
    val movimientos: StateFlow<List<MovimientoEntity>> = _userEmail.flatMapLatest { email ->
        repository.obtenerPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalGastos: StateFlow<Double> = _userEmail.flatMapLatest { email ->
        repository.obtenerTotalGastosPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIngresos: StateFlow<Double> = _userEmail.flatMapLatest { email ->
        repository.obtenerTotalIngresosPorUsuario(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Saldo inicial registrado de la cuenta
    val saldoInicial: StateFlow<Double> = _userEmail.flatMapLatest { email ->
        callbackFlow {
            if (email.isBlank()) {
                trySend(sessionManager.obtenerSaldoInicial())
                awaitClose { }
                return@callbackFlow
            }
            val listener = db.collection("cuentas").document(email)
                .addSnapshotListener { snap: DocumentSnapshot?, _ ->
                    val valSaldo = snap?.getDouble("saldoInicial") ?: sessionManager.obtenerSaldoInicial()
                    trySend(valSaldo)
                }
            awaitClose { listener.remove() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), sessionManager.obtenerSaldoInicial())

    fun actualizarEmail(email: String) {
        _userEmail.value = email
    }

    fun mostrarDialogoLogout() {
        _showLogoutDialog.value = true
    }

    fun ocultarDialogoLogout() {
        _showLogoutDialog.value = false
    }
}
