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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solvex.solvex.ui.theme.BackgroundColor
import com.solvex.solvex.ui.theme.BorderColor
import com.solvex.solvex.ui.theme.GreenPrimary
import com.solvex.solvex.ui.theme.NavyPrimary
import com.solvex.solvex.ui.theme.TextDark
import com.solvex.solvex.ui.theme.TextMuted
import com.solvex.solvex.ui.theme.TextSecondary

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginClick: (email: String, pass: String) -> Unit = { _, _ -> },
    onGoogleLoginClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Logo ($ Circle)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(NavyPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Green Tagline
        Text(
            text = "TU FINANZA, EN UN SOLO LUGAR",
            color = GreenPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = "Bienvenido de vuelta",
            color = TextDark,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle
        Text(
            text = "Inicia sesión para revisar tus finanzas.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email Field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Correo electrónico",
                color = TextDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = {
                    Text(
                        text = "nombre@correo.com",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Password Field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Contraseña",
                color = TextDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text(
                        text = "••••••••",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Text(
                        text = if (isPasswordVisible) "Ocultar" else "Mostrar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { isPasswordVisible = !isPasswordVisible }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Remember Me & Forgot Password Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { rememberMe = !rememberMe }
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = GreenPrimary,
                        uncheckedColor = BorderColor,
                        checkmarkColor = Color.White
                    )
                )
                Text(
                    text = "Recordarme",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            Text(
                text = "¿Olvidaste tu contraseña?",
                color = GreenPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onForgotPasswordClick() }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Login Button
        Button(
            onClick = { onLoginClick(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NavyPrimary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Iniciar sesión",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = BorderColor
            )
            Text(
                text = "o continúa con",
                modifier = Modifier.padding(horizontal = 12.dp),
                color = TextMuted,
                fontSize = 13.sp
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = BorderColor
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Google Button
        OutlinedButton(
            onClick = onGoogleLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = TextDark
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleLogo(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continuar con Google",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Register Link
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "¿Aún no tienes una cuenta? ",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Text(
                text = "Regístrate",
                color = GreenPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Path Blue
        val pathBlue = Path().apply {
            moveTo(width * 0.95f, height * 0.5f)
            cubicTo(width * 0.95f, height * 0.43f, width * 0.93f, height * 0.36f, width * 0.91f, height * 0.30f)
            lineTo(width * 0.5f, height * 0.30f)
            lineTo(width * 0.5f, height * 0.49f)
            lineTo(width * 0.76f, height * 0.49f)
            cubicTo(width * 0.75f, height * 0.57f, width * 0.70f, height * 0.65f, width * 0.62f, height * 0.70f)
            lineTo(width * 0.62f, height * 0.87f)
            lineTo(width * 0.78f, height * 0.87f)
            cubicTo(width * 0.88f, height * 0.78f, width * 0.95f, height * 0.65f, width * 0.95f, height * 0.5f)
            close()
        }
        drawPath(pathBlue, color = Color(0xFF4285F4))

        // Path Green
        val pathGreen = Path().apply {
            moveTo(width * 0.5f, height * 0.95f)
            cubicTo(width * 0.64f, height * 0.95f, width * 0.76f, height * 0.90f, width * 0.84f, height * 0.83f)
            lineTo(width * 0.68f, height * 0.71f)
            cubicTo(width * 0.63f, height * 0.74f, width * 0.57f, height * 0.76f, width * 0.5f, height * 0.76f)
            cubicTo(width * 0.37f, height * 0.76f, width * 0.26f, height * 0.67f, width * 0.22f, height * 0.55f)
            lineTo(width * 0.06f, height * 0.55f)
            lineTo(width * 0.06f, height * 0.68f)
            cubicTo(width * 0.14f, height * 0.84f, width * 0.31f, height * 0.95f, width * 0.5f, height * 0.95f)
            close()
        }
        drawPath(pathGreen, color = Color(0xFF34A853))

        // Path Yellow
        val pathYellow = Path().apply {
            moveTo(width * 0.22f, height * 0.55f)
            cubicTo(width * 0.20f, height * 0.50f, width * 0.20f, height * 0.45f, width * 0.22f, height * 0.40f)
            lineTo(width * 0.22f, height * 0.27f)
            lineTo(width * 0.06f, height * 0.27f)
            cubicTo(width * 0.02f, height * 0.34f, 0f, height * 0.42f, 0f, height * 0.50f)
            cubicTo(0f, height * 0.58f, width * 0.02f, height * 0.66f, width * 0.06f, height * 0.73f)
            lineTo(width * 0.22f, height * 0.60f)
            lineTo(width * 0.22f, height * 0.55f)
            close()
        }
        drawPath(pathYellow, color = Color(0xFFFBBC05))

        // Path Red
        val pathRed = Path().apply {
            moveTo(width * 0.5f, height * 0.24f)
            cubicTo(width * 0.58f, height * 0.24f, width * 0.65f, height * 0.27f, width * 0.71f, height * 0.32f)
            lineTo(width * 0.83f, height * 0.20f)
            cubicTo(width * 0.75f, height * 0.12f, width * 0.63f, height * 0.05f, width * 0.5f, height * 0.05f)
            cubicTo(width * 0.31f, height * 0.05f, width * 0.14f, height * 0.16f, width * 0.06f, height * 0.32f)
            lineTo(width * 0.22f, height * 0.44f)
            cubicTo(width * 0.26f, height * 0.32f, width * 0.37f, height * 0.24f, width * 0.5f, height * 0.24f)
            close()
        }
        drawPath(pathRed, color = Color(0xFFEA4335))
    }
}
