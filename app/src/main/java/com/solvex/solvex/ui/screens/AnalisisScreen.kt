package com.solvex.solvex.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solvex.solvex.ui.theme.GreenPrimary
import com.solvex.solvex.ui.theme.NavyPrimary
import com.solvex.solvex.ui.theme.TextMuted
import com.solvex.solvex.ui.viewmodel.AnalisisViewModel
import com.solvex.solvex.ui.viewmodel.CategoriaBarra
import com.solvex.solvex.ui.viewmodel.PresupuestoCategoria
import com.solvex.solvex.ui.viewmodel.SuscripcionElemento
import java.util.Locale

private fun cop(monto: Double): String =
    "$" + String.format(Locale.US, "%,.0f", monto).replace(',', '.')

private val coloresCategorias = mapOf(
    "Comida y Restaurantes" to Color(0xFF10B981),
    "Supermercados y Mercado" to Color(0xFF059669),
    "Transporte y Gasolina" to Color(0xFF3B82F6),
    "Entretenimiento" to Color(0xFF8B5CF6),
    "Compras y Comercio" to Color(0xFFF59E0B),
    "Facturas y Servicios" to Color(0xFFEC4899),
    "Pago de Tarjetas" to Color(0xFF6366F1),
    "Salud y Droguerías" to Color(0xFF06B6D4),
    "Bolsillos y Ahorro" to Color(0xFF14B8A6),
    "Transferencias y Envíos" to Color(0xFF64748B),
    "Ingresos y Rendimientos" to Color(0xFF22C55E),
    "Comisiones e Impuestos" to Color(0xFFEF4444),
    "Otro" to Color(0xFF94A3B8)
)

@Composable
fun AnalisisScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalisisViewModel,
    userEmail: String = "",
    onEditarLimites: () -> Unit = {}
) {
    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) viewModel.actualizarEmail(userEmail)
    }

    val periodoSel by viewModel.periodoSeleccionado.collectAsState()
    val barrasCategorias by viewModel.barrasCategorias.collectAsState()
    val presupuestosActivos by viewModel.presupuestosActivos.collectAsState()
    val suscripciones by viewModel.suscripcionesYGastosHormiga.collectAsState()

    val periodos = listOf("Semana", "Mes", "Año")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título "Análisis Financiero"
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Análisis Financiero", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
        }

        // Selector de período: Capsule tabs ("Semana", "Mes", "Año")
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFF1F5F9)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    periodos.forEachIndexed { index, label ->
                        val seleccionado = periodoSel == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (seleccionado) NavyPrimary else Color.Transparent)
                                .clickable { viewModel.cambiarPeriodo(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                                color = if (seleccionado) Color.White else TextMuted
                            )
                        }
                    }
                }
            }
        }

        // Card 1: Gastos por categoría (Gráfico de Barras Verticales)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Gastos por categoría",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (barrasCategorias.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay datos de gastos registradas para este periodo.",
                                fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Gráfico de Barras Verticales con bordes redondeados
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            barrasCategorias.forEach { barra: CategoriaBarra ->
                                ColumnaBarraVertical(
                                    nombre = barra.nombreAbr,
                                    proporcion = barra.proporcion,
                                    esRojo = barra.esRojo
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 2: Presupuestos activos
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Presupuestos activos",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Editar límites",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenPrimary,
                            modifier = Modifier.clickable { onEditarLimites() }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        presupuestosActivos.forEach { p: PresupuestoCategoria ->
                            ElementoPresupuesto(presupuesto = p)
                        }
                    }
                }
            }
        }

        // Card 3: Gastos hormiga & Suscripciones
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gastos hormiga & Suscripciones",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        suscripciones.forEachIndexed { idx: Int, s: SuscripcionElemento ->
                            ElementoSuscripcion(suscripcion = s)
                            if (idx < suscripciones.size - 1) {
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

// Columna individual del gráfico de barras estilo exacto del diseño
@Composable
private fun ColumnaBarraVertical(nombre: String, proporcion: Float, esRojo: Boolean) {
    val colorBarra = if (esRojo) Color(0xFFE53E3E) else Color(0xFF22C55E)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(95.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFFF8FAFC)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(proporcion.coerceIn(0.1f, 1.0f))
                    .clip(RoundedCornerShape(13.dp))
                    .background(colorBarra)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = nombre,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

// Elemento individual de un presupuesto activo
@Composable
private fun ElementoPresupuesto(presupuesto: PresupuestoCategoria) {
    val colorProgreso = when {
        presupuesto.porcentaje >= 100 -> Color(0xFFE53E3E)
        presupuesto.porcentaje >= 85 -> Color(0xFFF59E0B)
        else -> Color(0xFF22C55E)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(presupuesto.categoria, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${cop(presupuesto.gastado)} de ",
                    fontSize = 12.sp, color = TextMuted
                )
                Text(
                    text = cop(presupuesto.limite),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${presupuesto.porcentaje}%",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorProgreso
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF8FAFC))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth( (presupuesto.porcentaje / 100f).coerceIn(0.02f, 1.0f) )
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorProgreso)
            )
        }
    }
}

// Elemento individual de suscripciones / gastos hormiga
@Composable
private fun ElementoSuscripcion(suscripcion: SuscripcionElemento) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (bgColor, iconColor) = when (suscripcion.iconoTipo) {
                "netflix" -> Pair(Color(0xFFFFF5F5), Color(0xFFE53E3E))
                "spotify" -> Pair(Color(0xFFF0FDF4), Color(0xFF22C55E))
                "icloud" -> Pair(Color(0xFFFFFBEB), Color(0xFFF59E0B))
                else -> Pair(Color(0xFFF1F5F9), NavyPrimary)
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                when (suscripcion.iconoTipo) {
                    "netflix" -> CameraIcon(modifier = Modifier.size(18.dp), color = iconColor)
                    "spotify" -> MusicIcon(modifier = Modifier.size(18.dp), color = iconColor)
                    "icloud" -> CloudIcon(modifier = Modifier.size(18.dp), color = iconColor)
                    else -> GeneralStarIcon(modifier = Modifier.size(18.dp), color = iconColor)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = suscripcion.titulo,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "${cop(suscripcion.monto)} / mes",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = NavyPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun CameraIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.35f)
            lineTo(w * 0.8f, h * 0.35f)
            lineTo(w * 0.8f, h * 0.75f)
            lineTo(w * 0.2f, h * 0.75f)
            close()
        }
        drawPath(path, color = color, style = stroke)
    }
}

@Composable
private fun MusicIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawLine(color, Offset(w * 0.35f, h * 0.25f), Offset(w * 0.75f, h * 0.15f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.35f, h * 0.25f), Offset(w * 0.35f, h * 0.70f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.28f, h * 0.70f))
    }
}

@Composable
private fun CloudIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color, radius = w * 0.20f, center = Offset(w * 0.5f, h * 0.45f), style = stroke)
        drawCircle(color, radius = w * 0.14f, center = Offset(w * 0.3f, h * 0.55f), style = stroke)
        drawCircle(color, radius = w * 0.14f, center = Offset(w * 0.7f, h * 0.55f), style = stroke)
    }
}

@Composable
private fun GeneralStarIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawCircle(color, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f))
    }
}
