package com.solvex.solvex.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import com.solvex.solvex.ui.theme.GreenPrimary
import com.solvex.solvex.ui.theme.NavyPrimary
import com.solvex.solvex.ui.theme.RedAlert
import com.solvex.solvex.ui.theme.TextMuted
import com.solvex.solvex.ui.viewmodel.PerfilViewModel

@Composable
fun AjustesEntidadesScreen(
    modifier: Modifier = Modifier,
    viewModel: PerfilViewModel,
    onBackClick: () -> Unit = {},
    onContactarSoporte: () -> Unit = {},
    onLimitesGasto: () -> Unit = {}
) {
    val notificaciones by viewModel.notificacionesActivas.collectAsState()
    val actualizacionAuto by viewModel.actualizacionAutoActiva.collectAsState()
    val ingresosAnio by viewModel.ingresosAnioActual.collectAsState()
    val porcentajeTopeRenta by viewModel.porcentajeTopeRenta.collectAsState()
    val operacionEnCurso by viewModel.operacionEnCurso.collectAsState()
    val error by viewModel.error.collectAsState()
    var confirmarDesvincular by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.toggleNotificaciones(granted)
    }

    if (confirmarDesvincular) {
        AlertDialog(
            onDismissRequest = { if (!operacionEnCurso) confirmarDesvincular = false },
            title = { Text("¿Desvincular todas las cuentas?") },
            text = { Text("Se eliminarán de Solvex los movimientos, saldos e historial de extractos asociados a tu perfil. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    enabled = !operacionEnCurso,
                    onClick = {
                        viewModel.desvincularTodasLasCuentas {
                            confirmarDesvincular = false
                            onBackClick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) { Text(if (operacionEnCurso) "Desvinculando…" else "Desvincular", color = Color.White) }
            },
            dismissButton = {
                TextButton(enabled = !operacionEnCurso, onClick = { confirmarDesvincular = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Header con Botón de Regresar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                ArrowLeftIcon(modifier = Modifier.size(18.dp), color = NavyPrimary)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Ajustes de Entidades",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (error.isNotBlank()) {
            Text(error, color = RedAlert, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
        }

        // Tarjeta con las Opciones de Ajuste
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column {
                // Item 1: Notificaciones de movimientos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notificaciones de movimientos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Movimientos nuevos y umbrales de ingresos para renta",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = notificaciones,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else viewModel.toggleNotificaciones(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GreenPrimary
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Item 2: Actualización automática
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Actualización automática",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Actualizar el saldo guardado cada 4 horas",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = actualizacionAuto,
                        onCheckedChange = { viewModel.toggleActualizacionAuto(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GreenPrimary
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Item 3: Límites de gasto por cuenta
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLimitesGasto() }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NavyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            SlidersIcon(modifier = Modifier.size(16.dp), color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Límites de gasto por cuenta",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    }
                    ChevronRightIcon(modifier = Modifier.size(16.dp), color = TextMuted)
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Item 4: Contactar Soporte
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContactarSoporte() }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF22C55E)),
                            contentAlignment = Alignment.Center
                        ) {
                            HeadphonesIcon(modifier = Modifier.size(16.dp), color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Contactar Soporte",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    }
                    ChevronRightIcon(modifier = Modifier.size(16.dp), color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                Text("SEGUIMIENTO DE INGRESOS PARA RENTA 2026", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${currency(ingresosAnio)} de ${currency(PerfilViewModel.TOPE_INGRESOS_RENTA_2026)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (ingresosAnio / PerfilViewModel.TOPE_INGRESOS_RENTA_2026).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (porcentajeTopeRenta >= 100) RedAlert else GreenPrimary,
                    trackColor = GreenPrimary.copy(alpha = 0.12f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("$porcentajeTopeRenta% del umbral orientativo (1.400 UVT).", fontSize = 12.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    "Se calcula con los movimientos de ingreso registrados en Solvex durante 2026. Las notificaciones se envían al 50%, 80% y 100%. La DIAN también evalúa otros requisitos para declarar.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón Peligro: Desvincular todas las cuentas
        Button(
            onClick = { confirmarDesvincular = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFF5F5),
                contentColor = RedAlert
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text(
                text = "Desvincular todas las cuentas",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = RedAlert
            )
        }
    }
}

private fun currency(amount: Double): String =
    java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO")).format(amount)

@Composable
private fun ArrowLeftIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(w * 0.65f, h * 0.2f)
            lineTo(w * 0.35f, h * 0.5f)
            lineTo(w * 0.65f, h * 0.8f)
        }
        drawPath(path, color = color, style = stroke)
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

@Composable
private fun SlidersIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.12f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.2f, h * 0.3f), Offset(w * 0.8f, h * 0.3f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.2f, h * 0.7f), Offset(w * 0.8f, h * 0.7f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.4f, h * 0.3f))
        drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.6f, h * 0.7f))
    }
}

@Composable
private fun HeadphonesIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val arc = Path().apply {
            moveTo(w * 0.2f, h * 0.6f)
            cubicTo(w * 0.2f, h * 0.2f, w * 0.8f, h * 0.2f, w * 0.8f, h * 0.6f)
        }
        drawPath(arc, color = color, style = stroke)
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.2f, h * 0.65f))
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.8f, h * 0.65f))
    }
}
