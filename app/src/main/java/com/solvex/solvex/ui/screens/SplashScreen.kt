package com.solvex.solvex.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// color de fondo azul oscuro del splash
private val SplashBackground = Color(0xFF0F2544)
// color del recuadro del icono
private val IconBackground = Color(0xFF091A2F)
// color verde menta de las barras
private val GreenBarsColor = Color(0xFF5ECAA7)

// pantalla de inicio o splash que muestra el logo de solvex
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onSplashFinished: () -> Unit = {}
) {
    // animaciones para que el logo aparezca suavemente
    val escala = remember { Animatable(0.8f) }
    val opacidad = remember { Animatable(0f) }

    // efecto que corre la animacion y luego espera 2 segundos
    LaunchedEffect(Unit) {
        // animacion de entrada
        escala.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        // animacion de opacidad
        opacidad.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        // tiempo de espera antes de ir a la siguiente pantalla
        delay(1400)
        onSplashFinished()
    }

    // contenedor centrado con fondo azul oscuro
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(escala.value)
                .alpha(opacidad.value)
        ) {
            // icono central con las barras y la flecha blanca
            SolvexLogoCard(size = 130.dp)

            Spacer(modifier = Modifier.height(28.dp))

            // nombre de la app en blanco
            Text(
                text = "solvex",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // lema o descripcion inferior
            Text(
                text = "finanzas personales",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.8.sp
            )
        }
    }
}

// recuadro redondeado del logo con 3 barras verdes y flecha hacia arriba
@Composable
fun SolvexLogoCard(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.26f))
            .background(IconBackground),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val radioEsquina = CornerRadius(w * 0.035f, w * 0.035f)

            // barra 1 izquierda baja
            drawRoundRect(
                color = GreenBarsColor,
                topLeft = Offset(w * 0.26f, h * 0.56f),
                size = Size(w * 0.125f, h * 0.27f),
                cornerRadius = radioEsquina
            )

            // barra 2 centro mediana
            drawRoundRect(
                color = GreenBarsColor,
                topLeft = Offset(w * 0.44f, h * 0.39f),
                size = Size(w * 0.125f, h * 0.44f),
                cornerRadius = radioEsquina
            )

            // barra 3 derecha alta
            drawRoundRect(
                color = GreenBarsColor,
                topLeft = Offset(w * 0.62f, h * 0.26f),
                size = Size(w * 0.125f, h * 0.57f),
                cornerRadius = radioEsquina
            )

            // trazo blanco grueso para la flecha
            val grosor = w * 0.078f
            val estiloLinea = Stroke(
                width = grosor,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

            // puntos del recorrido de la flecha
            val pInicio = Offset(w * 0.24f, h * 0.63f)
            val pPico1 = Offset(w * 0.44f, h * 0.43f)
            val pValle = Offset(w * 0.53f, h * 0.52f)
            val pPunta = Offset(w * 0.77f, h * 0.23f)

            // linea que sube en zigzag
            val rutaLinea = Path().apply {
                moveTo(pInicio.x, pInicio.y)
                lineTo(pPico1.x, pPico1.y)
                lineTo(pValle.x, pValle.y)
                lineTo(pPunta.x, pPunta.y)
            }
            drawPath(rutaLinea, color = Color.White, style = estiloLinea)

            // punta de flecha en la esquina superior derecha
            val tamanoPunta = w * 0.14f
            val rutaPunta = Path().apply {
                moveTo(pPunta.x - tamanoPunta, pPunta.y)
                lineTo(pPunta.x, pPunta.y)
                lineTo(pPunta.x, pPunta.y + tamanoPunta)
            }
            drawPath(rutaPunta, color = Color.White, style = estiloLinea)
        }
    }
}
