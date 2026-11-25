package com.solvex.solvex.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solvex.solvex.ui.theme.GreenPrimary
import com.solvex.solvex.ui.theme.NavyPrimary
import com.solvex.solvex.ui.theme.TextMuted
import com.solvex.solvex.ui.viewmodel.AnalisisViewModel
import com.solvex.solvex.ui.viewmodel.AsistenteViewModel
import com.solvex.solvex.ui.viewmodel.DashboardViewModel
import com.solvex.solvex.ui.viewmodel.ExtractoViewModel
import com.solvex.solvex.ui.viewmodel.MovimientosViewModel
import com.solvex.solvex.ui.viewmodel.PerfilViewModel

// Pantalla principal con navegación entre pestañas e sub-pantallas de Perfil / Ajustes / Soporte
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    userName: String = "Carlos",
    userEmail: String = "",
    dashboardViewModel: DashboardViewModel,
    movimientosViewModel: MovimientosViewModel,
    extractoViewModel: ExtractoViewModel,
    analisisViewModel: AnalisisViewModel,
    asistenteViewModel: AsistenteViewModel,
    perfilViewModel: PerfilViewModel,
    demoMode: Boolean = false,
    onLogout: () -> Unit = {}
) {
    // tab seleccionada (0=inicio, 1=movimientos, 2=extracto, 3=analisis, 4=asistente)
    var selectedTab by remember { mutableIntStateOf(0) }

    // sub-pantallas de Perfil (0 = Pestaña principal, 1 = Perfil, 2 = Ajustes, 3 = Soporte)
    var subPantalla by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            // Barra inferior solo si estamos en las pestañas principales
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Surface(
                    shadowElevation = 12.dp,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavItem(
                            label = "Inicio",
                            isSelected = selectedTab == 0 && subPantalla == 0,
                            onClick = {
                                selectedTab = 0
                                subPantalla = 0
                            }
                        ) { color ->
                            HomeNavIcon(modifier = Modifier.size(22.dp), color = color)
                        }

                        NavItem(
                            label = "Movimientos",
                            isSelected = selectedTab == 1 && subPantalla == 0,
                            onClick = {
                                selectedTab = 1
                                subPantalla = 0
                            }
                        ) { color ->
                            MovimientosNavIcon(modifier = Modifier.size(22.dp), color = color)
                        }

                        Spacer(modifier = Modifier.size(60.dp))

                        NavItem(
                            label = "Análisis",
                            isSelected = selectedTab == 3 && subPantalla == 0,
                            onClick = {
                                selectedTab = 3
                                subPantalla = 0
                            }
                        ) { color ->
                            AnalisisNavIcon(modifier = Modifier.size(22.dp), color = color)
                        }

                        NavItem(
                            label = "Asistente",
                            isSelected = selectedTab == 4 && subPantalla == 0,
                            onClick = {
                                selectedTab = 4
                                subPantalla = 0
                            }
                        ) { color ->
                            AsistenteNavIcon(modifier = Modifier.size(22.dp), color = color)
                        }
                    }
                }

                // Botón flotante central
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-16).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(NavyPrimary)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                selectedTab = 2
                                subPantalla = 0
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val w = size.width; val h = size.height
                            val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            val doc = Path().apply {
                                moveTo(w * 0.22f, h * 0.12f)
                                lineTo(w * 0.58f, h * 0.12f)
                                lineTo(w * 0.78f, h * 0.32f)
                                lineTo(w * 0.78f, h * 0.88f)
                                lineTo(w * 0.22f, h * 0.88f)
                                close()
                            }
                            drawPath(doc, color = GreenPrimary, style = stroke)

                            val arrow = Path().apply {
                                moveTo(w * 0.50f, h * 0.72f)
                                lineTo(w * 0.50f, h * 0.42f)
                                moveTo(w * 0.38f, h * 0.52f)
                                lineTo(w * 0.50f, h * 0.40f)
                                lineTo(w * 0.62f, h * 0.52f)
                            }
                            drawPath(arrow, color = GreenPrimary, style = stroke)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (demoMode) {
                Surface(color = Color(0xFFFFF3CD), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "MODO DEMO LOCAL · datos de muestra; no se guardan en la nube",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = Color(0xFF744210),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (subPantalla == 1) {
                // Imagen 1: Perfil / Entidades Vinculadas
                PerfilScreen(
                    viewModel = perfilViewModel,
                    userName = userName,
                    userEmail = userEmail,
                    onNavigateAjustes = { subPantalla = 2 },
                    onVincularNuevaCuenta = {
                        selectedTab = 2
                        subPantalla = 0
                    },
                    onHistorialExtractos = {
                        selectedTab = 2
                        subPantalla = 0
                    },
                    onLogoutRequest = onLogout
                )
            } else if (subPantalla == 2) {
                // Imagen 2: Ajustes de Entidades
                AjustesEntidadesScreen(
                    viewModel = perfilViewModel,
                    onBackClick = { subPantalla = 1 },
                    onContactarSoporte = { subPantalla = 3 },
                    onLimitesGasto = { subPantalla = 4 }
                )
            } else if (subPantalla == 3) {
                // Imagen 3: Contactar Soporte
                ContactarSoporteScreen(
                    onBackClick = { subPantalla = 2 },
                    onChatEnVivo = {
                        selectedTab = 4
                        subPantalla = 0
                    }
                )
            } else if (subPantalla == 4) {
                LimitesGastoScreen(
                    viewModel = perfilViewModel,
                    onBackClick = { subPantalla = 2 }
                )
            } else if (subPantalla == 5) {
                LimitesGastoScreen(
                    viewModel = perfilViewModel,
                    onBackClick = { subPantalla = 0 }
                )
            } else {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        viewModel = dashboardViewModel,
                        userName = userName,
                        userEmail = userEmail,
                        onLogoutRequest = { subPantalla = 1 }, // Al hacer clic en avatar va a Perfil
                        onNavigateToMovimientos = { selectedTab = 1 }
                    )
                    1 -> MovimientosScreen(
                        viewModel = movimientosViewModel,
                        userEmail = userEmail
                    )
                    2 -> ExtractoScreen(
                        viewModel = extractoViewModel,
                        userEmail = userEmail,
                        onCerrarClick = { selectedTab = 0 },
                        onMovimientosGuardados = { selectedTab = 1 }
                    )
                    3 -> AnalisisScreen(
                        viewModel = analisisViewModel,
                        userEmail = userEmail,
                        onEditarLimites = { subPantalla = 5 }
                    )
                    4 -> AsistenteScreen(viewModel = asistenteViewModel, userName = userName, userEmail = userEmail)
                    else -> DashboardScreen(
                        viewModel = dashboardViewModel,
                        userName = userName,
                        userEmail = userEmail,
                        onLogoutRequest = { subPantalla = 1 },
                        onNavigateToMovimientos = { selectedTab = 1 }
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (color: Color) -> Unit
) {
    val activeColor = if (isSelected) GreenPrimary else TextMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 6.dp)
    ) {
        icon(activeColor)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = activeColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun HomeNavIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val roof = Path().apply {
            moveTo(w * 0.15f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.85f, h * 0.48f)
            lineTo(w * 0.85f, h * 0.85f)
            lineTo(w * 0.15f, h * 0.85f)
            close()
        }
        drawPath(roof, color = color, style = stroke)

        val door = Path().apply {
            moveTo(w * 0.38f, h * 0.85f)
            lineTo(w * 0.38f, h * 0.60f)
            lineTo(w * 0.62f, h * 0.60f)
            lineTo(w * 0.62f, h * 0.85f)
        }
        drawPath(door, color = color, style = stroke)
    }
}

@Composable
private fun MovimientosNavIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawLine(color, Offset(w * 0.15f, h * 0.28f), Offset(w * 0.30f, h * 0.28f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.40f, h * 0.28f), Offset(w * 0.85f, h * 0.28f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)

        drawLine(color, Offset(w * 0.15f, h * 0.50f), Offset(w * 0.30f, h * 0.50f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.40f, h * 0.50f), Offset(w * 0.85f, h * 0.50f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)

        drawLine(color, Offset(w * 0.15f, h * 0.72f), Offset(w * 0.30f, h * 0.72f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.40f, h * 0.50f), Offset(w * 0.85f, h * 0.72f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
private fun AnalisisNavIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(w * 0.12f, h * 0.12f),
            size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.76f),
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        )
        drawLine(color, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.88f, h * 0.50f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.50f, h * 0.12f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
private fun AsistenteNavIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val bubble = Path().apply {
            moveTo(w * 0.20f, h * 0.20f)
            lineTo(w * 0.80f, h * 0.20f)
            lineTo(w * 0.80f, h * 0.65f)
            lineTo(w * 0.45f, h * 0.65f)
            lineTo(w * 0.25f, h * 0.82f)
            lineTo(w * 0.25f, h * 0.65f)
            lineTo(w * 0.20f, h * 0.65f)
            close()
        }
        drawPath(bubble, color = color, style = stroke)
    }
}
