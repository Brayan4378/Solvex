package com.solvex.solvex.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.solvex.solvex.ui.theme.BackgroundColor
import com.solvex.solvex.ui.theme.GreenPrimary
import com.solvex.solvex.ui.theme.NavyPrimary
import com.solvex.solvex.ui.theme.TextDark
import com.solvex.solvex.ui.theme.TextMuted
import com.solvex.solvex.ui.viewmodel.DashboardViewModel
import com.solvex.solvex.ui.viewmodel.MovimientosViewModel

// pantalla principal con barra inferior y 4 tabs + boton central
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    userName: String = "Carlos",
    userEmail: String = "",
    dashboardViewModel: DashboardViewModel,
    movimientosViewModel: MovimientosViewModel,
    onLogout: () -> Unit = {}
) {
    // tab seleccionada (0=inicio, 1=movimientos, 2=analisis, 3=asistente)
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            // barra inferior personalizada
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // fondo blanco de la barra
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
                        // tab inicio
                        NavItem(
                            label = "Inicio",
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        ) { color ->
                            HomeNavIcon(modifier = Modifier.size(22.dp), color = color)
                        }

                        // tab movimientos
                        NavItem(
                            label = "Movimientos",
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        ) { color ->
                            MovimientosNavIcon(modifier = Modifier.size(22.dp), color = color)
                        }

                        // espacio vacio para el boton central
                        Spacer(modifier = Modifier.size(60.dp))

                        // tab analisis
                        NavItem(
                            label = "Analisis",
                            isSelected = selectedTab == 2,
                            onClick = { selectedTab = 2 }
                        ) { color ->
                            AnalisisNavIcon(modifier = Modifier.size(22.dp), color = color)
                        }

                        // tab asistente
                        NavItem(
                            label = "Asistente",
                            isSelected = selectedTab == 3,
                            onClick = { selectedTab = 3 }
                        ) { color ->
                            AsistenteNavIcon(modifier = Modifier.size(22.dp), color = color)
                        }
                    }
                }

                // boton central flotante que sobresale de la barra
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
                                // al presionar el boton central va a la pantalla de movimientos
                                selectedTab = 1
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // icono de documento con flecha arriba
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val w = size.width
                            val h = size.height
                            val stroke = Stroke(
                                width = w * 0.09f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )

                            // forma del documento
                            val doc = Path().apply {
                                moveTo(w * 0.22f, h * 0.12f)
                                lineTo(w * 0.58f, h * 0.12f)
                                lineTo(w * 0.78f, h * 0.32f)
                                lineTo(w * 0.78f, h * 0.88f)
                                lineTo(w * 0.22f, h * 0.88f)
                                close()
                            }
                            drawPath(doc, color = GreenPrimary, style = stroke)

                            // flecha hacia arriba dentro del documento
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    viewModel = dashboardViewModel,
                    userName = userName,
                    userEmail = userEmail,
                    onLogoutRequest = onLogout,
                    onNavigateToMovimientos = { selectedTab = 1 }
                )
                1 -> MovimientosScreen(
                    viewModel = movimientosViewModel,
                    userEmail = userEmail
                )
                2 -> PantallaProximamente(titulo = "Analisis")
                3 -> PantallaProximamente(titulo = "Asistente")
                else -> DashboardScreen(
                    viewModel = dashboardViewModel,
                    userName = userName,
                    userEmail = userEmail,
                    onLogoutRequest = onLogout,
                    onNavigateToMovimientos = { selectedTab = 1 }
                )
            }
        }
    }
}

// cada elemento de la barra inferior
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

// icono de casa para la tab inicio
@Composable
private fun HomeNavIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // forma de la casa con techo y paredes
        val roof = Path().apply {
            moveTo(w * 0.15f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.85f, h * 0.48f)
            lineTo(w * 0.85f, h * 0.85f)
            lineTo(w * 0.15f, h * 0.85f)
            close()
        }
        drawPath(roof, color = color, style = stroke)

        // puerta en el centro
        val door = Path().apply {
            moveTo(w * 0.38f, h * 0.85f)
            lineTo(w * 0.38f, h * 0.60f)
            lineTo(w * 0.62f, h * 0.60f)
            lineTo(w * 0.62f, h * 0.85f)
        }
        drawPath(door, color = color, style = stroke)
    }
}

// icono de lista para la tab movimientos
@Composable
private fun MovimientosNavIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // tres lineas con puntos a la izquierda (como una lista)
        drawLine(color, Offset(w * 0.15f, h * 0.28f), Offset(w * 0.30f, h * 0.28f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.40f, h * 0.28f), Offset(w * 0.85f, h * 0.28f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)

        drawLine(color, Offset(w * 0.15f, h * 0.50f), Offset(w * 0.30f, h * 0.50f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.40f, h * 0.50f), Offset(w * 0.85f, h * 0.50f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)

        drawLine(color, Offset(w * 0.15f, h * 0.72f), Offset(w * 0.30f, h * 0.72f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.40f, h * 0.72f), Offset(w * 0.85f, h * 0.72f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

// icono de grafico circular para la tab analisis
@Composable
private fun AnalisisNavIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // circulo con lineas radiales (como un pie chart)
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

// icono de burbuja de chat para la tab asistente
@Composable
private fun AsistenteNavIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // forma de burbuja de conversacion
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

// pantalla temporal para las secciones que aun no estan listas
@Composable
private fun PantallaProximamente(titulo: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = titulo,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Proximamente",
            fontSize = 14.sp,
            color = TextMuted
        )
    }
}
