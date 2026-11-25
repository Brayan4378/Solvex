package com.solvex.solvex.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solvex.solvex.ui.theme.BackgroundColor
import com.solvex.solvex.ui.theme.BorderColor
import com.solvex.solvex.ui.theme.GreenLight
import com.solvex.solvex.ui.theme.GreenPrimary
import com.solvex.solvex.ui.theme.NavyPrimary
import com.solvex.solvex.ui.theme.OrangeAlert
import com.solvex.solvex.ui.theme.RedAlert
import com.solvex.solvex.ui.theme.TextDark
import com.solvex.solvex.ui.theme.TextMuted
import com.solvex.solvex.ui.theme.TextSecondary
import com.solvex.solvex.ui.viewmodel.ExtractoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatearMonto(monto: Double): String {
    return "$" + String.format(Locale.US, "%,.0f", monto).replace(',', '.')
}

@Composable
fun ExtractoScreen(
    modifier: Modifier = Modifier,
    viewModel: ExtractoViewModel,
    userEmail: String = "",
    onCerrarClick: () -> Unit = {},
    onMovimientosGuardados: () -> Unit = {}
) {
    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) {
            viewModel.actualizarEmail(userEmail)
        }
    }

    val cargando by viewModel.cargando.collectAsState()
    val resultado by viewModel.extractoResultado.collectAsState()
    val nombreArchivo by viewModel.nombreArchivo.collectAsState()
    val cargasRecientes by viewModel.cargasRecientes.collectAsState()
    val bancoDetectado by viewModel.bancoDetectado.collectAsState()
    val guardadoExitoso by viewModel.guardadoExitoso.collectAsState()
    val guardandoMovimientos by viewModel.guardandoMovimientos.collectAsState()
    val pdfSolicitaContrasena by viewModel.pdfSolicitaContrasena.collectAsState()
    val avisoCarga by viewModel.avisoCarga.collectAsState()
    val errorGuardado by viewModel.errorGuardado.collectAsState()

    var mostrarPegarTexto by remember { mutableStateOf(false) }
    var textoPegado by remember { mutableStateOf("") }
    var contrasenaPDF by remember { mutableStateOf("") }
    var uriPendiente by remember { mutableStateOf<Uri?>(null) }
    var nombrePendiente by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val nombre = viewModel.obtenerNombreReal(uri)
            uriPendiente = uri
            nombrePendiente = nombre
            viewModel.procesarExtracto(uri, nombre, contrasenaPDF)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.procesarExtracto(uri, "foto_extracto.jpg")
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "CO")) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Barra superior
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onCerrarClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "×", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
                Text(text = "Subir Extracto", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.size(36.dp))
            }
        }

        // 1. Zona de carga
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.5.dp, GreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(vertical = 24.dp, horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(26.dp)) {
                            val w = size.width; val h = size.height
                            val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            val arrow = Path().apply {
                                moveTo(w * 0.5f, h * 0.15f)
                                lineTo(w * 0.5f, h * 0.65f)
                                moveTo(w * 0.3f, h * 0.35f)
                                lineTo(w * 0.5f, h * 0.15f)
                                lineTo(w * 0.7f, h * 0.35f)
                            }
                            drawPath(arrow, color = GreenPrimary, style = stroke)
                            drawLine(GreenPrimary, Offset(w * 0.2f, h * 0.85f), Offset(w * 0.8f, h * 0.85f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Sube tu extracto bancario", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Soportamos PDFs de Davivienda, Bancolombia, Nequi, BBVA y más.\nEl banco y sus saldos se detectan automáticamente.",
                        fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center,
                        lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (pdfSolicitaContrasena) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Este PDF está protegido. Ingresa la contraseña para continuar.", fontSize = 12.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = contrasenaPDF,
                            onValueChange = { contrasenaPDF = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Contraseña del PDF (ej: cédula)", fontSize = 12.sp, color = TextMuted) },
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.procesarExtracto(uriPendiente, nombrePendiente, contrasenaPDF) },
                            enabled = !cargando && contrasenaPDF.isNotBlank() && uriPendiente != null,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Text("Continuar con contraseña", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { filePicker.launch(arrayOf("application/pdf", "text/*", "*/*")) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = Color.White)
                    ) {
                        Text("Seleccionar archivo PDF", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = NavyPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Text("Tomar foto del extracto", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = { mostrarPegarTexto = !mostrarPegarTexto },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (mostrarPegarTexto) GreenPrimary.copy(alpha = 0.08f) else Color.White,
                            contentColor = GreenPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary)
                    ) {
                        Text(
                            text = if (mostrarPegarTexto) "Ocultar área de texto" else "Pegar texto del extracto",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GreenPrimary
                        )
                    }

                    if (mostrarPegarTexto) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = textoPegado,
                            onValueChange = { textoPegado = it },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            placeholder = {
                                Text(
                                    "Pega aquí el texto de tu extracto...\n\nEj: 01 10 $ 5,000.00- 4445 Transferencia enviada a Daviplata",
                                    fontSize = 11.sp, color = TextMuted, lineHeight = 15.sp
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor),
                            maxLines = 20
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (textoPegado.isNotBlank()) {
                                    viewModel.procesarTextoDirecto(textoPegado)
                                    mostrarPegarTexto = false
                                }
                            },
                            enabled = textoPegado.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Text("Analizar texto pegado", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        if (avisoCarga.isNotBlank() && resultado?.movimientosExtraidos?.isNotEmpty() != true) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OrangeAlert)
                ) {
                    Text(
                        avisoCarga,
                        modifier = Modifier.padding(14.dp),
                        fontSize = 12.sp,
                        color = TextDark,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // 2. Banco detectado
        if (bancoDetectado.isNotBlank()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("B", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Entidad detectada", fontSize = 11.sp, color = TextMuted)
                            Text(text = bancoDetectado, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                        }
                    }
                }
            }
        }

        // 3. Progreso de Carga
        if (cargando) {
            item {
                val progreso by animateFloatAsState(targetValue = 0.70f, label = "progreso")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            text = if (nombreArchivo.isNotBlank()) "Procesando \"$nombreArchivo\"" else "Analizando extracto...",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progreso },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = GreenPrimary,
                            trackColor = GreenLight.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        // 4. Resultado del Análisis
        val analizado = resultado
        if (analizado != null && !cargando) {
            if (analizado.movimientosExtraidos.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = NavyPrimary)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Resultado del Análisis", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            if (analizado.periodo.isNotBlank()) {
                                Text("Periodo: ${analizado.periodo}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SALDO INICIAL", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                                    Text(formatearMonto(analizado.saldoInicial), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SALDO FINAL", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                                    Text(formatearMonto(analizado.saldoFinal), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenLight)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = analizado.resumenIA,
                                fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { viewModel.guardarMovimientosEnNube { onMovimientosGuardados() } },
                                enabled = !guardadoExitoso && !guardandoMovimientos,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                            ) {
                                Text(
                                    text = when {
                                        guardadoExitoso -> "✓ ${analizado.movimientosExtraidos.size} movimientos guardados — Ver Movimientos"
                                        guardandoMovimientos -> "Guardando movimientos…"
                                        else -> "Guardar ${analizado.movimientosExtraidos.size} movimientos en Movimientos"
                                    },
                                    fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp
                                )
                            }
                            if (errorGuardado.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(errorGuardado, fontSize = 12.sp, color = Color(0xFFFFB4AB), lineHeight = 16.sp)
                            }
                            if (avisoCarga.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(avisoCarga, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), lineHeight = 16.sp)
                            }
                        }
                    }
                }

                // Lista de movimientos clasificados
                items(analizado.movimientosExtraidos.take(12)) { mov ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val colorCategoria = when (mov.categoria) {
                                "Entretenimiento" -> Color(0xFF8B5CF6)
                                "Compras y Comercio" -> Color(0xFFF59E0B)
                                "Comida y Restaurantes" -> Color(0xFF10B981)
                                "Supermercados y Mercado" -> Color(0xFF059669)
                                "Transporte y Gasolina" -> Color(0xFF3B82F6)
                                "Facturas y Servicios" -> Color(0xFFEC4899)
                                "Pago de Tarjetas" -> Color(0xFF6366F1)
                                "Salud y Droguerías" -> Color(0xFF06B6D4)
                                "Bolsillos y Ahorro" -> Color(0xFF14B8A6)
                                "Ingresos y Rendimientos" -> GreenPrimary
                                "Transferencias y Envíos" -> Color(0xFF64748B)
                                else -> TextMuted
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colorCategoria.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mov.categoria.take(1),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorCategoria
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mov.titulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(mov.categoria, fontSize = 11.sp, color = colorCategoria, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                text = "${if (mov.tipo == "gasto") "-" else "+"}${formatearMonto(mov.monto)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mov.tipo == "gasto") RedAlert else GreenPrimary
                            )
                        }
                    }
                }

                if (analizado.movimientosExtraidos.size > 12) {
                    item {
                        Text(
                            "y ${analizado.movimientosExtraidos.size - 12} movimientos más",
                            fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else if (analizado.resumenIA.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangeAlert)
                    ) {
                        Text(
                            text = analizado.resumenIA,
                            fontSize = 13.sp, color = TextDark, lineHeight = 18.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    if (uriPendiente != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.procesarExtracto(uriPendiente, nombrePendiente, contrasenaPDF)
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Text("Reintentar lectura del extracto", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // 5. Cargas recientes
        if (cargasRecientes.isNotEmpty()) {
            item {
                Text(
                    text = "CARGAS RECIENTES",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp
                )
            }

            items(cargasRecientes) { carga ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NavyPrimary.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("PDF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    carga.nombreArchivo,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${carga.banco} · ${carga.totalMovimientos} mov. · ${
                                        if (carga.fechaProcesado > 0) dateFormat.format(Date(carga.fechaProcesado)) else ""
                                    }",
                                    fontSize = 11.sp, color = TextMuted
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GreenPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = carga.estado,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
