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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solvex.solvex.ui.theme.BackgroundColor
import com.solvex.solvex.ui.theme.BorderColor
import com.solvex.solvex.ui.theme.ChartBlue
import com.solvex.solvex.ui.theme.ChartGreen
import com.solvex.solvex.ui.theme.ChartOrange
import com.solvex.solvex.ui.theme.ChartRed
import com.solvex.solvex.ui.theme.GreenLight
import com.solvex.solvex.ui.theme.GreenPrimary
import com.solvex.solvex.ui.theme.NavyPrimary
import com.solvex.solvex.ui.theme.OrangeAlert
import com.solvex.solvex.ui.theme.OrangeLight
import com.solvex.solvex.ui.theme.RedAlert
import com.solvex.solvex.ui.theme.RedLight
import com.solvex.solvex.ui.theme.TextDark
import com.solvex.solvex.ui.theme.TextMuted
import com.solvex.solvex.ui.theme.TextSecondary
import com.solvex.solvex.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// formatea un numero como moneda colombiana (ejemplo: 4200000 -> $4.200.000)
private fun formatearMonto(monto: Double): String {
    val valorAbsoluto = abs(monto)
    val formateado = "$" + String.format(Locale.US, "%,.0f", valorAbsoluto).replace(',', '.')
    return if (monto < 0) "-$formateado" else formateado
}

// formatea la fecha del timestamp para las alertas
private fun formatearFechaCorta(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.forLanguageTag("es-CO"))
    return sdf.format(Date(timestamp))
}

// pantalla principal del dashboard que observa el viewmodel
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel,
    userName: String = "Carlos",
    userEmail: String = "",
    onLogoutRequest: () -> Unit = {},
    onNavigateToMovimientos: () -> Unit = {}
) {
    // sincronizar email con el viewmodel
    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) {
            viewModel.actualizarEmail(userEmail)
        }
    }

    // observar estados del viewmodel
    val totalGastos by viewModel.totalGastos.collectAsState()
    val totalIngresos by viewModel.totalIngresos.collectAsState()
    val movimientos by viewModel.movimientos.collectAsState()
    val showLogoutDialog by viewModel.showLogoutDialog.collectAsState()

    // calculamos el saldo real restando gastos de ingresos
    val saldoConsolidado = totalIngresos - totalGastos
    val tieneMovimientos = movimientos.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
    ) {
        // encabezado con saludo y avatar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Bienvenido de vuelta",
                fontSize = 12.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hola, $userName \uD83D\uDC4B",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                // circulo con la inicial que al hacer click abre dialogo de cerrar sesion
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NavyPrimary)
                        .clickable { viewModel.mostrarDialogoLogout() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ventana emergente de confirmacion para cerrar sesion
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.ocultarDialogoLogout() },
                title = {
                    Text(
                        text = "Cerrar sesión",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark
                    )
                },
                text = {
                    Text(
                        text = "¿Estás seguro de que deseas cerrar la sesión actual?",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.ocultarDialogoLogout()
                            onLogoutRequest()
                        }
                    ) {
                        Text(
                            text = "Sí, cerrar sesión",
                            color = RedAlert,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.ocultarDialogoLogout() }
                    ) {
                        Text(
                            text = "Cancelar",
                            color = TextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // tarjeta oscura con el saldo real calculado de room
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable { onNavigateToMovimientos() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavyPrimary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SALDO CONSOLIDADO TOTAL",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (tieneMovimientos) "${movimientos.size} Movimientos" else "0 Movimientos",
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // saldo real calculado de ingresos menos gastos
                Text(
                    text = formatearMonto(saldoConsolidado),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (tieneMovimientos) "COP \u2022 Actualizado en tiempo real" else "COP \u2022 Sin movimientos registrados",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                Spacer(modifier = Modifier.height(16.dp))

                // seccion inferior con totales de ingresos y gastos reales
                Row(modifier = Modifier.fillMaxWidth()) {
                    // columna de ingresos
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "\u2191", fontSize = 14.sp, color = GreenLight)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ingresos",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+${formatearMonto(totalIngresos)}",
                            fontSize = 14.sp,
                            color = GreenLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // columna de gastos
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "\u2193", fontSize = 14.sp, color = RedAlert)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gastos",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-${formatearMonto(totalGastos)}",
                            fontSize = 14.sp,
                            color = RedAlert,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // tarjeta de resumen de gastos
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "\uD83D\uDCCA", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Resumen de gastos",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (totalGastos > 0) "Has registrado ${formatearMonto(totalGastos)} en gastos" else "Registra gastos en Movimientos para ver tus metricas",
                        fontSize = 13.sp,
                        color = TextDark
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (totalGastos > 0) RedLight else BorderColor.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (totalGastos > 0) "-${formatearMonto(totalGastos)}" else "$0",
                        fontSize = 11.sp,
                        color = if (totalGastos > 0) RedAlert else TextMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // seccion de movimientos o alertas recientes en tiempo real
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ultimos movimientos",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Ver todos",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenPrimary,
                    modifier = Modifier.clickable { onNavigateToMovimientos() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tieneMovimientos) {
                // mostramos hasta los 2 movimientos mas recientes guardados en room
                val recientes = movimientos.take(2)
                recientes.forEach { item ->
                    val esGasto = item.tipo == "gasto"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (esGasto) RedLight else GreenLight.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (esGasto) "\uD83D\uDCC9" else "\uD83D\uDCC8",
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.categoria.uppercase(),
                                    fontSize = 10.sp,
                                    color = if (esGasto) RedAlert else GreenPrimary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.titulo,
                                    fontSize = 13.sp,
                                    color = TextDark
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (esGasto) "-${formatearMonto(item.monto)}" else "+${formatearMonto(item.monto)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (esGasto) RedAlert else GreenPrimary
                                )
                                Text(
                                    text = formatearFechaCorta(item.fecha),
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            } else {
                // estado cuando todavia no hay movimientos guardados
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BorderColor.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "\uD83D\uDD14", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SIN MOVIMIENTOS",
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tus gastos recientes apareceran aqui",
                                fontSize = 13.sp,
                                color = TextDark
                            )
                        }
                    }
                }
            }
        }

        // seccion de gastos por categoria con grafico circular real
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gastos por Categoria",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Administrar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenPrimary,
                    modifier = Modifier.clickable { onNavigateToMovimientos() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // calculo dinamico de los gastos por categoria segun room
            val listaGastos = remember(movimientos) { movimientos.filter { it.tipo == "gasto" } }
            val sumaTotalGastos = remember(listaGastos) { listaGastos.sumOf { it.monto } }

            val categoriasCalculadas = remember(listaGastos, sumaTotalGastos) {
                if (sumaTotalGastos > 0) {
                    listaGastos.groupBy { it.categoria }
                        .map { (cat, items) ->
                            val subtotal = items.sumOf { it.monto }
                            val porcentaje = ((subtotal / sumaTotalGastos) * 100).toFloat()
                            val color = when (cat.lowercase()) {
                                "comida" -> ChartGreen
                                "transporte" -> ChartBlue
                                "entretenimiento" -> ChartOrange
                                "servicios" -> ChartRed
                                else -> Color(0xFF8B5CF6)
                            }
                            Triple(cat, porcentaje, color)
                        }
                        .sortedByDescending { it.second }
                } else {
                    emptyList()
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val grosor = 24f
                            val estilo = Stroke(width = grosor, cap = StrokeCap.Round)
                            val diametro = size.minDimension - grosor
                            val esquina = Offset(grosor / 2f, grosor / 2f)
                            val tamano = Size(diametro, diametro)

                            if (categoriasCalculadas.isNotEmpty()) {
                                var angulo = -90f
                                categoriasCalculadas.forEach { (_, porcentaje, color) ->
                                    val barrido = (porcentaje / 100f) * 360f
                                    if (barrido > 3f) {
                                        drawArc(
                                            color = color,
                                            startAngle = angulo,
                                            sweepAngle = barrido - 2f,
                                            useCenter = false,
                                            topLeft = esquina,
                                            size = tamano,
                                            style = estilo
                                        )
                                    }
                                    angulo += barrido
                                }
                            } else {
                                // circulo vacio si no hay gastos
                                drawArc(
                                    color = BorderColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = esquina,
                                    size = tamano,
                                    style = estilo
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Gastos",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Text(
                                text = formatearMonto(totalGastos),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    if (categoriasCalculadas.isNotEmpty()) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categoriasCalculadas.take(4).forEach { (cat, porcentaje, color) ->
                                LeyendaItem(
                                    texto = "$cat (${String.format(Locale.US, "%.0f", porcentaje)}%)",
                                    color = color
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Sin gastos registrados",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Agrega gastos para ver tu distribucion",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

// elemento de leyenda para cada categoria del grafico
@Composable
private fun LeyendaItem(texto: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = texto,
            fontSize = 11.sp,
            color = TextDark,
            fontWeight = FontWeight.Medium
        )
    }
}
