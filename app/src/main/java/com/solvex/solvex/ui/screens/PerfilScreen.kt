package com.solvex.solvex.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solvex.solvex.ui.theme.GreenLight
import com.solvex.solvex.ui.theme.GreenPrimary
import com.solvex.solvex.ui.theme.NavyPrimary
import com.solvex.solvex.ui.theme.RedAlert
import com.solvex.solvex.ui.theme.TextMuted
import com.solvex.solvex.ui.viewmodel.EntidadBancaria
import com.solvex.solvex.ui.viewmodel.PerfilViewModel
import java.util.Locale

private fun cop(monto: Double): String =
    "$" + String.format(Locale.US, "%,.0f", monto).replace(',', '.')

@Composable
fun PerfilScreen(
    modifier: Modifier = Modifier,
    viewModel: PerfilViewModel,
    userName: String = "Carlos Méndez",
    userEmail: String = "carlos.mendez@solvex.com",
    onNavigateAjustes: () -> Unit = {},
    onVincularNuevaCuenta: () -> Unit = {},
    onHistorialExtractos: () -> Unit = {},
    onLogoutRequest: () -> Unit = {}
) {
    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) viewModel.actualizarEmail(userEmail)
    }

    val entidades by viewModel.entidadesVinculadas.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera del Perfil con Foto/Avatar
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(NavyPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (userName.contains(" ")) userName else "$userName Méndez",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (userEmail.isNotBlank()) userEmail else "carlos.mendez@solvex.com",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }
        }

        // Sección: Entidades Vinculadas
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Entidades Vinculadas",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Text(
                    text = "Ajustes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenPrimary,
                    modifier = Modifier.clickable { onNavigateAjustes() }
                )
            }
        }

        // Tarjetas de entidades bancarias vinculadas
        items(entidades) { entidad ->
            TarjetaEntidadBancaria(entidad = entidad)
        }

        // Botón: Vincular nueva cuenta bancaria
        item {
            Button(
                onClick = { onVincularNuevaCuenta() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenLight.copy(alpha = 0.35f),
                    contentColor = GreenPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Vincular nueva cuenta bancaria",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                }
            }
        }

        // Sección: PREFERENCIAS
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "PREFERENCIAS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column {
                    // Item 1: Seguridad y huella digital
                    FilaPreferencia(
                        titulo = "Seguridad y huella digital",
                        iconoStr = "security",
                        valorDerecho = "Activo",
                        esCerrarSesion = false,
                        onClick = { }
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Item 2: Configurar alertas y notificaciones
                    FilaPreferencia(
                        titulo = "Configurar alertas y notificaciones",
                        iconoStr = "notifications",
                        esCerrarSesion = false,
                        onClick = { onNavigateAjustes() }
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Item 3: Historial de extractos cargados
                    FilaPreferencia(
                        titulo = "Historial de extractos cargados",
                        iconoStr = "history",
                        esCerrarSesion = false,
                        onClick = { onHistorialExtractos() }
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Item 4: Cerrar sesión
                    FilaPreferencia(
                        titulo = "Cerrar sesión",
                        iconoStr = "logout",
                        esCerrarSesion = true,
                        onClick = { onLogoutRequest() }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

@Composable
private fun TarjetaEntidadBancaria(entidad: EntidadBancaria) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo/Ícono del Banco
                val (bgColor, iconChar, textColor) = when (entidad.icono) {
                    "bancolombia" -> Triple(Color(0xFFFFE600), "B", NavyPrimary)
                    "davivienda" -> Triple(Color(0xFFED1C24), "D", Color.White)
                    "nequi" -> Triple(Color(0xFF330033), "N", Color.White)
                    else -> Triple(Color(0xFFF1F5F9), "S", NavyPrimary)
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconChar,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = entidad.nombre,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entidad.tipoCuenta,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Text(
                text = cop(entidad.saldo),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        }
    }
}

@Composable
private fun FilaPreferencia(
    titulo: String,
    iconoStr: String,
    valorDerecho: String? = null,
    esCerrarSesion: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (esCerrarSesion) Color(0xFFFFF5F5) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                when (iconoStr) {
                    "security" -> SecurityIcon(modifier = Modifier.size(16.dp), color = NavyPrimary)
                    "notifications" -> BellIcon(modifier = Modifier.size(16.dp), color = NavyPrimary)
                    "history" -> HistoryIcon(modifier = Modifier.size(16.dp), color = NavyPrimary)
                    else -> LogoutIcon(modifier = Modifier.size(16.dp), color = RedAlert)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (esCerrarSesion) RedAlert else NavyPrimary
            )
        }

        if (valorDerecho != null) {
            Text(
                text = valorDerecho,
                fontSize = 12.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
        } else {
            ChevronRightIcon(modifier = Modifier.size(16.dp), color = if (esCerrarSesion) RedAlert else TextMuted)
        }
    }
}

@Composable
private fun SecurityIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
        drawLine(color, Offset(w * 0.35f, h * 0.5f), Offset(w * 0.65f, h * 0.5f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
    }
}

@Composable
private fun BellIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val bell = Path().apply {
            moveTo(w * 0.3f, h * 0.65f)
            lineTo(w * 0.7f, h * 0.65f)
            lineTo(w * 0.65f, h * 0.35f)
            cubicTo(w * 0.65f, h * 0.2f, w * 0.35f, h * 0.2f, w * 0.35f, h * 0.35f)
            close()
        }
        drawPath(bell, color = color, style = stroke)
    }
}

@Composable
private fun HistoryIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawArc(color, startAngle = 0f, sweepAngle = 270f, useCenter = false, topLeft = Offset(w * 0.15f, h * 0.15f), size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.7f), style = stroke)
        drawLine(color, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.3f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.7f, h * 0.5f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
    }
}

@Composable
private fun LogoutIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(color, Offset(w * 0.2f, h * 0.2f), Offset(w * 0.2f, h * 0.8f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.4f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
    }
}

@Composable
private fun ChevronRightIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.2f)
            lineTo(w * 0.65f, h * 0.5f)
            lineTo(w * 0.35f, h * 0.8f)
        }
        drawPath(path, color = color, style = stroke)
    }
}
