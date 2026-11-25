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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solvex.solvex.data.entity.MovimientoEntity
import com.solvex.solvex.ui.theme.BackgroundColor
import com.solvex.solvex.ui.theme.BorderColor
import com.solvex.solvex.ui.theme.GreenLight
import com.solvex.solvex.ui.theme.GreenPrimary
import com.solvex.solvex.ui.theme.NavyPrimary
import com.solvex.solvex.ui.theme.RedAlert
import com.solvex.solvex.ui.theme.RedLight
import com.solvex.solvex.ui.theme.TextDark
import com.solvex.solvex.ui.theme.TextMuted
import com.solvex.solvex.ui.theme.TextSecondary
import com.solvex.solvex.ui.viewmodel.MovimientosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// formatea numeros a formato de moneda colombiana
private fun formatearMonto(monto: Double): String {
    return "$" + String.format(Locale.US, "%,.0f", monto).replace(',', '.')
}

// formatea la fecha del timestamp a texto entendible
private fun formatearFecha(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.forLanguageTag("es-CO"))
    return sdf.format(Date(timestamp))
}

// lista completa de categorías detalladas para gastos e ingresos
private val categoriasGastos = listOf(
    "Comida y Restaurantes",
    "Supermercados y Mercado",
    "Transporte y Gasolina",
    "Entretenimiento",
    "Compras y Comercio",
    "Facturas y Servicios",
    "Pago de Tarjetas",
    "Salud y Droguerías",
    "Bolsillos y Ahorro",
    "Transferencias y Envíos",
    "Ingresos y Rendimientos",
    "Comisiones e Impuestos",
    "Otro"
)

// icono y color segun la categoria seleccionada
private fun obtenerDetallesCategoria(categoria: String): Pair<String, Color> {
    val c = categoria.lowercase(Locale.ROOT)
    return when {
        c.contains("restaurante") || c.contains("comida") -> Pair("🍔", Color(0xFF10B981))
        c.contains("supermercado") || c.contains("mercado") -> Pair("🛒", Color(0xFF059669))
        c.contains("transporte") || c.contains("gasolina") -> Pair("⛽", Color(0xFF3B82F6))
        c.contains("entretenimiento") || c.contains("suscripciones") -> Pair("🍿", Color(0xFF8B5CF6))
        c.contains("compras") || c.contains("comercio") || c.contains("hogar") -> Pair("🛍️", Color(0xFFF59E0B))
        c.contains("facturas") || c.contains("servicios") -> Pair("💡", Color(0xFFEC4899))
        c.contains("tarjeta") || c.contains("crédito") || c.contains("credito") -> Pair("💳", Color(0xFF6366F1))
        c.contains("salud") || c.contains("droguería") || c.contains("drogueria") -> Pair("💊", Color(0xFF06B6D4))
        c.contains("bolsillo") || c.contains("ahorro") -> Pair("🐷", Color(0xFF14B8A6))
        c.contains("transferencia") || c.contains("envío") || c.contains("envio") -> Pair("💸", Color(0xFF64748B))
        c.contains("ingreso") || c.contains("rendimiento") || c.contains("salario") -> Pair("💰", Color(0xFF22C55E))
        c.contains("comisión") || c.contains("comision") || c.contains("impuesto") || c.contains("gmf") -> Pair("🏦", Color(0xFFEF4444))
        else -> Pair("🏷️", Color(0xFF94A3B8))
    }
}

// pantalla que observa el viewmodel de movimientos
@Composable
fun MovimientosScreen(
    modifier: Modifier = Modifier,
    viewModel: MovimientosViewModel,
    userEmail: String = ""
) {
    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) {
            viewModel.actualizarEmail(userEmail)
        }
    }

    val movimientos by viewModel.movimientos.collectAsState()
    val totalGastos by viewModel.totalGastos.collectAsState()
    val totalIngresos by viewModel.totalIngresos.collectAsState()
    val filtroSeleccionado by viewModel.filtroSeleccionado.collectAsState()
    val mostrarDialogoFormulario by viewModel.mostrarDialogoFormulario.collectAsState()
    val movimientoAEditar by viewModel.movimientoAEditar.collectAsState()
    val movimientoAEliminar by viewModel.movimientoAEliminar.collectAsState()
    val mostrarDialogoEliminarTodo by viewModel.mostrarDialogoEliminarTodo.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val errorCarga by viewModel.errorCarga.collectAsState()
    val errorOperacion by viewModel.errorOperacion.collectAsState()
    val operacionEnCurso by viewModel.operacionEnCurso.collectAsState()

    val listaFiltrada = remember(movimientos, filtroSeleccionado) {
        when (filtroSeleccionado) {
            1 -> movimientos.filter { it.tipo == "gasto" }
            2 -> movimientos.filter { it.tipo == "ingreso" }
            else -> movimientos
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Movimientos",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Gasto e ingreso de la cuenta",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (movimientos.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF5F5))
                                .clickable { viewModel.solicitarEliminarTodo() },
                            contentAlignment = Alignment.Center
                        ) {
                            TrashIcon(modifier = Modifier.size(18.dp), color = RedAlert)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Button(
                        onClick = { viewModel.abrirFormularioNuevo() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Text(
                            text = "+ Nuevo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL GASTOS",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-${formatearMonto(totalGastos)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedAlert
                        )
                    }

                    Column {
                        Text(
                            text = "TOTAL INGRESOS",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+${formatearMonto(totalIngresos)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenLight
                        )
                    }

                    Column {
                        Text(
                            text = "BALANCE NETO",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val balance = totalIngresos - totalGastos
                        Text(
                            text = formatearMonto(balance),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (balance >= 0) Color.White else RedAlert
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroChipItem(
                    texto = "Todos (${movimientos.size})",
                    seleccionado = filtroSeleccionado == 0,
                    onClick = { viewModel.cambiarFiltro(0) }
                )
                FiltroChipItem(
                    texto = "Gastos",
                    seleccionado = filtroSeleccionado == 1,
                    onClick = { viewModel.cambiarFiltro(1) }
                )
                FiltroChipItem(
                    texto = "Ingresos",
                    seleccionado = filtroSeleccionado == 2,
                    onClick = { viewModel.cambiarFiltro(2) }
                )
            }
        }

        if (errorCarga.isNotBlank()) {
            Text(
                text = errorCarga,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        if (errorOperacion.isNotBlank() && !mostrarDialogoFormulario && movimientoAEliminar == null && !mostrarDialogoEliminarTodo) {
            Text(
                text = errorOperacion,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                color = RedAlert,
                fontSize = 12.sp
            )
        }

        if (listaFiltrada.isEmpty() && cargando) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopCenter) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GreenPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Conectando y cargando movimientos…", fontSize = 13.sp, color = TextMuted)
                }
            }
        } else if (listaFiltrada.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📝",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No hay movimientos registrados",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Agrega tu primer gasto o ingreso usando el botón superior.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { viewModel.abrirFormularioNuevo() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "+ Registrar gasto", color = GreenPrimary)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = listaFiltrada,
                    key = { mov: MovimientoEntity ->
                        if (mov.id != 0) mov.id else "${mov.titulo}_${mov.monto}_${mov.fecha}_${mov.hashCode()}"
                    }
                ) { item ->
                    MovimientoCardItem(
                        movimiento = item,
                        onEditar = { viewModel.abrirFormularioEditar(item) },
                        onEliminar = { viewModel.solicitarEliminar(item) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        }
    }

    if (mostrarDialogoFormulario) {
        DialogoMovimiento(
            movimientoExistente = movimientoAEditar,
            userEmail = userEmail.ifBlank { viewModel.userEmail.collectAsState().value },
            errorOperacion = errorOperacion,
            operacionEnCurso = operacionEnCurso,
            onDismiss = { viewModel.cerrarFormulario() },
            onGuardar = { nuevoMovimiento -> viewModel.guardarMovimiento(nuevoMovimiento) },
            onLimpiarError = { viewModel.limpiarErrorOperacion() }
        )
    }

    if (movimientoAEliminar != null) {
        val item = movimientoAEliminar!!
        AlertDialog(
            onDismissRequest = { viewModel.cancelarEliminar() },
            title = {
                Text(
                    text = "Eliminar movimiento",
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            },
            text = {
                Text(
                    text = errorOperacion.ifBlank { "Estas seguro de que deseas eliminar '${item.titulo}' por ${formatearMonto(item.monto)}?" },
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    enabled = !operacionEnCurso,
                    onClick = { viewModel.confirmarEliminar() },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) {
                    Text(text = if (operacionEnCurso) "Eliminando…" else "Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarEliminar() }) {
                    Text(text = "Cancelar", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    if (mostrarDialogoEliminarTodo) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelarEliminarTodo() },
            title = {
                Text(
                    text = "¿Eliminar todo el historial?",
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            },
            text = {
                Text(
                    text = "Esta acción eliminará permanentemente todos tus ${movimientos.size} movimientos e historial financiero. ¿Estás seguro de continuar?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    enabled = !operacionEnCurso,
                    onClick = { viewModel.confirmarEliminarTodo() },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) {
                    Text(text = "Sí, borrar todo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarEliminarTodo() }) {
                    Text(text = "Cancelar", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
private fun MovimientoCardItem(
    movimiento: MovimientoEntity,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val (emoji, colorCategoria) = obtenerDetallesCategoria(movimiento.categoria)
    val esGasto = movimiento.tipo == "gasto"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colorCategoria.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movimiento.titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = movimiento.categoria,
                        fontSize = 11.sp,
                        color = colorCategoria,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " • ",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = formatearFecha(movimiento.fecha),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (esGasto) "-${formatearMonto(movimiento.monto)}" else "+${formatearMonto(movimiento.monto)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (esGasto) RedAlert else GreenPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Editar",
                        fontSize = 11.sp,
                        color = NavyPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onEditar() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    Text(
                        text = "Borrar",
                        fontSize = 11.sp,
                        color = RedAlert,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onEliminar() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogoMovimiento(
    movimientoExistente: MovimientoEntity?,
    userEmail: String,
    errorOperacion: String,
    operacionEnCurso: Boolean,
    onDismiss: () -> Unit,
    onGuardar: (MovimientoEntity) -> Unit,
    onLimpiarError: () -> Unit
) {
    val esEdicion = movimientoExistente != null

    var titulo by remember { mutableStateOf(movimientoExistente?.titulo ?: "") }
    var montoTexto by remember {
        mutableStateOf(movimientoExistente?.monto?.let { String.format(Locale.US, "%.0f", it) } ?: "")
    }
    var tipo by remember { mutableStateOf(movimientoExistente?.tipo ?: "gasto") }
    var categoria by remember { mutableStateOf(movimientoExistente?.categoria ?: "Comida y Restaurantes") }
    var errorMensaje by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (esEdicion) "Editar movimiento" else "Nuevo movimiento",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (tipo == "gasto") RedLight else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tipo = "gasto" }
                    ) {
                        Text(
                            text = "Gasto",
                            color = if (tipo == "gasto") RedAlert else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (tipo == "ingreso") GreenLight.copy(alpha = 0.2f) else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tipo = "ingreso" }
                    ) {
                        Text(
                            text = "Ingreso",
                            color = if (tipo == "ingreso") GreenPrimary else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Descripcion",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it; onLimpiarError() },
                    placeholder = { Text("Ej: Almuerzo, Mercado, Terpel", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = BorderColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Monto en COP ($)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = montoTexto,
                    onValueChange = { montoTexto = it.filter { char -> char.isDigit() }; onLimpiarError() },
                    placeholder = { Text("Ej: 35000", color = TextMuted, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = BorderColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Categoria",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoriasGastos) { cat ->
                        val seleccionada = categoria == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (seleccionada) GreenPrimary else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { categoria = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                color = if (seleccionada) Color.White else TextDark,
                                fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                val errorVisible = errorOperacion.ifBlank { errorMensaje }
                if (errorVisible.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorVisible,
                        color = RedAlert,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !operacionEnCurso,
                onClick = {
                    val montoDouble = montoTexto.toDoubleOrNull()
                    if (titulo.isBlank()) {
                        errorMensaje = "Ingresa una descripcion"
                        return@Button
                    }
                    if (montoDouble == null || montoDouble <= 0) {
                        errorMensaje = "Ingresa un monto valido mayor a 0"
                        return@Button
                    }

                    val movimientoFinal = MovimientoEntity(
                        id = movimientoExistente?.id ?: 0,
                        usuarioEmail = movimientoExistente?.usuarioEmail ?: userEmail,
                        titulo = titulo.trim(),
                        monto = montoDouble,
                        categoria = categoria,
                        tipo = tipo,
                        fecha = movimientoExistente?.fecha ?: System.currentTimeMillis()
                    )
                    onGuardar(movimientoFinal)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text(
                    text = if (operacionEnCurso) "Guardando…" else if (esEdicion) "Actualizar" else "Guardar",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

@Composable
private fun FiltroChipItem(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (seleccionado) NavyPrimary else Color.White,
        border = if (seleccionado) null else androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = texto,
            fontSize = 12.sp,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
            color = if (seleccionado) Color.White else TextDark,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TrashIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(color, Offset(w * 0.2f, h * 0.25f), Offset(w * 0.8f, h * 0.25f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.4f, h * 0.15f), Offset(w * 0.6f, h * 0.15f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
        val body = Path().apply {
            moveTo(w * 0.28f, h * 0.25f)
            lineTo(w * 0.32f, h * 0.85f)
            lineTo(w * 0.68f, h * 0.85f)
            lineTo(w * 0.72f, h * 0.25f)
        }
        drawPath(body, color = color, style = stroke)
        drawLine(color, Offset(w * 0.43f, h * 0.40f), Offset(w * 0.43f, h * 0.72f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.57f, h * 0.40f), Offset(w * 0.57f, h * 0.72f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
    }
}
