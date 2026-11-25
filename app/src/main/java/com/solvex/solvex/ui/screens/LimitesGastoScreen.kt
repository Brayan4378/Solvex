package com.solvex.solvex.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solvex.solvex.ui.theme.GreenPrimary
import com.solvex.solvex.ui.theme.NavyPrimary
import com.solvex.solvex.ui.theme.TextMuted
import com.solvex.solvex.ui.viewmodel.PerfilViewModel

@Composable
fun LimitesGastoScreen(
    viewModel: PerfilViewModel,
    onBackClick: () -> Unit
) {
    val limites by viewModel.limites.collectAsState()
    val valores = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(limites) {
        limites.forEach { (categoria, monto) ->
            if (!valores.containsKey(categoria)) valores[categoria] = monto.toLong().toString()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFF1F5F9))
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) { Text("‹", fontSize = 28.sp, color = NavyPrimary) }
                Text("Límites de gasto", modifier = Modifier.padding(start = 16.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
            }
            Text("Define topes mensuales por categoría. El análisis comparará tus gastos con estos valores.", modifier = Modifier.padding(top = 12.dp), fontSize = 13.sp, color = TextMuted)
        }

        items(PerfilViewModel.CATEGORIAS_PRESUPUESTO) { categoria ->
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(categoria, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                    OutlinedTextField(
                        value = valores[categoria].orEmpty(),
                        onValueChange = { entrada -> valores[categoria] = entrada.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        prefix = { Text("$", color = TextMuted) },
                        suffix = { Text("COP", color = TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    val saved = PerfilViewModel.CATEGORIAS_PRESUPUESTO.associateWith { categoria ->
                        valores[categoria]?.toDoubleOrNull() ?: limites[categoria] ?: PerfilViewModel.limitePredeterminado(categoria)
                    }
                    viewModel.guardarLimites(saved)
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) { Text("Guardar límites", fontWeight = FontWeight.Bold, color = Color.White) }
            Spacer(Modifier.height(90.dp))
        }
    }
}
