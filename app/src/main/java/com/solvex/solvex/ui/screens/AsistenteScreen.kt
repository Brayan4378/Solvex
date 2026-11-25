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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solvex.solvex.ui.theme.GreenPrimary
import com.solvex.solvex.ui.theme.NavyPrimary
import com.solvex.solvex.ui.theme.TextDark
import com.solvex.solvex.ui.theme.TextMuted
import com.solvex.solvex.ui.viewmodel.AsistenteViewModel
import com.solvex.solvex.ui.viewmodel.MensajeChat

@Composable
fun AsistenteScreen(
    modifier: Modifier = Modifier,
    viewModel: AsistenteViewModel,
    userName: String = "Carlos",
    userEmail: String = ""
) {
    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) viewModel.actualizarEmail(userEmail)
    }

    val mensajes by viewModel.mensajes.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    var textoInput by remember { mutableStateOf("") }
    var mostrarConfirmacionLimpiar by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val preguntasFrecuentes = remember {
        listOf(
            "¿Cuánto gasté en Terpel este mes?",
            "¿En qué categoría gasté más en octubre?",
            "¿Cuánto recibí este mes?",
            "Muéstrame mis últimos movimientos"
        )
    }

    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) {
            listState.animateScrollToItem(mensajes.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
    ) {
        if (mostrarConfirmacionLimpiar) {
            AlertDialog(
                onDismissRequest = { mostrarConfirmacionLimpiar = false },
                title = { Text("¿Eliminar el historial del chat?") },
                text = { Text("Se borrará esta conversación y se iniciará un chat nuevo.") },
                confirmButton = {
                    Button(
                        onClick = {
                            mostrarConfirmacionLimpiar = false
                            viewModel.limpiarHistorial()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) { Text("Eliminar historial", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarConfirmacionLimpiar = false }) { Text("Cancelar") }
                }
            )
        }

        // Header de Solvex AI
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(NavyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            SolvexBrainIcon(modifier = Modifier.size(24.dp), color = GreenPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Solvex AI", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(GreenPrimary)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("En línea • Asistente virtual", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                        .clickable { mostrarConfirmacionLimpiar = true },
                        contentAlignment = Alignment.Center
                    ) {
                        GearIcon(modifier = Modifier.size(20.dp), color = TextMuted)
                    }
                }
                HorizontalDivider(color = Color(0xFFF1F5F9))
            }
        }

        // Historial de mensajes
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            items(mensajes) { mensaje ->
                BurbujaMensaje(mensaje = mensaje)
            }

            if (cargando) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            shadowElevation = 1.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = NavyPrimary
                                )
                                Text("Solvex AI está analizando tus datos...", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // Área inferior: Chips + Campo de texto estilo prototipo
        Surface(
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(preguntasFrecuentes) { chip ->
                        val chipStr = chip.toString()
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable {
                                viewModel.enviarMensaje(chipStr, userName)
                            }
                        ) {
                            Text(
                                text = chipStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = NavyPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textoInput,
                        onValueChange = { textoInput = it },
                        placeholder = {
                            Text("Pregúntale a Solvex AI...", fontSize = 14.sp, color = TextMuted)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(26.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2E8F0),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textoInput.isNotBlank() && !cargando) {
                                    viewModel.enviarMensaje(textoInput, userName)
                                    textoInput = ""
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TextDark)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary)
                            .clickable(enabled = textoInput.isNotBlank() && !cargando) {
                                viewModel.enviarMensaje(textoInput, userName)
                                textoInput = ""
                                keyboardController?.hide()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        SendPlaneIcon(modifier = Modifier.size(20.dp), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun BurbujaMensaje(mensaje: MensajeChat) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mensaje.esUsuario) Arrangement.End else Arrangement.Start
    ) {
        if (!mensaje.esUsuario) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NavyPrimary),
                contentAlignment = Alignment.Center
            ) {
                SolvexBrainIcon(modifier = Modifier.size(18.dp), color = GreenPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (mensaje.esUsuario) Alignment.End else Alignment.Start) {
            Surface(
                modifier = Modifier.widthIn(max = 280.dp),
                shape = RoundedCornerShape(
                    topStart = if (mensaje.esUsuario) 18.dp else 4.dp,
                    topEnd = if (mensaje.esUsuario) 4.dp else 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                ),
                color = if (mensaje.esUsuario) NavyPrimary else Color.White,
                shadowElevation = if (mensaje.esUsuario) 0.dp else 1.dp,
                border = if (!mensaje.esUsuario) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null
            ) {
                Text(
                    text = if (mensaje.esUsuario) AnnotatedString(mensaje.texto) else formatearTextoRespuestaIA(mensaje.texto),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (mensaje.esUsuario) Color.White else TextDark
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = mensaje.timestamp,
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

private fun formatearTextoRespuestaIA(texto: String): AnnotatedString {
    return buildAnnotatedString {
        val palabras = texto.split(" ")
        palabras.forEachIndexed { index, palabra ->
            val pLimpia = palabra.replace(",", "").replace(".", "").replace(":", "")

            when {
                palabra.startsWith("$") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextDark)) {
                        append(palabra)
                    }
                }
                pLimpia.equals("Comida", ignoreCase = true) || pLimpia.equals("Transporte", ignoreCase = true) ||
                        pLimpia.equals("Entretenimiento", ignoreCase = true) -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFE53E3E))) {
                        append(palabra)
                    }
                }
                palabra.contains("%") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))) {
                        append(palabra)
                    }
                }
                else -> append(palabra)
            }
            if (index < palabras.size - 1) append(" ")
        }
    }
}

@Composable
private fun SolvexBrainIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val path = Path().apply {
            moveTo(w * 0.3f, h * 0.2f)
            cubicTo(w * 0.1f, h * 0.3f, w * 0.1f, h * 0.7f, w * 0.35f, h * 0.8f)
            cubicTo(w * 0.5f, h * 0.9f, w * 0.7f, h * 0.85f, w * 0.8f, h * 0.65f)
            cubicTo(w * 0.9f, h * 0.45f, w * 0.75f, h * 0.2f, w * 0.5f, h * 0.2f)
            close()
        }
        drawPath(path, color = color, style = stroke)
        drawLine(color, Offset(w * 0.35f, h * 0.5f), Offset(w * 0.65f, h * 0.5f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
private fun GearIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.28f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
        drawLine(color, Offset(w * 0.5f, h * 0.1f), Offset(w * 0.5f, h * 0.22f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.5f, h * 0.78f), Offset(w * 0.5f, h * 0.9f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.1f, h * 0.5f), Offset(w * 0.22f, h * 0.5f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.78f, h * 0.5f), Offset(w * 0.9f, h * 0.5f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
private fun SendPlaneIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val plane = Path().apply {
            moveTo(w * 0.15f, h * 0.50f)
            lineTo(w * 0.85f, h * 0.18f)
            lineTo(w * 0.58f, h * 0.85f)
            lineTo(w * 0.45f, h * 0.55f)
            close()
        }
        drawPath(plane, color = color, style = stroke)
    }
}
