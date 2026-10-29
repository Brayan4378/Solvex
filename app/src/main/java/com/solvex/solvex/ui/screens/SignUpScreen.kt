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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.solvex.solvex.ui.theme.RedAlert
import com.solvex.solvex.ui.theme.TextDark
import com.solvex.solvex.ui.theme.TextMuted
import com.solvex.solvex.ui.theme.TextSecondary

@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier,
    errorMessage: String = "",
    onSignUpClick: (fullName: String, email: String, pass: String) -> Unit = { _, _, _ -> },
    onGoogleSignUpClick: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // Back Button Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                BackArrowIcon(modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Content Column
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Crea tu cuenta",
                color = TextDark,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Empieza a tomar el control de tus finanzas.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Cartel de error (si existe)
            if (errorMessage.isNotEmpty()) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = RedAlert,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Full Name Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Nombre completo",
                    color = TextDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = {
                        Text(
                            text = "Tu nombre",
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

            Spacer(modifier = Modifier.height(18.dp))

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
                        focusedBorderColor = if (errorMessage.contains("correo", ignoreCase = true)) RedAlert else GreenPrimary,
                        unfocusedBorderColor = if (errorMessage.contains("correo", ignoreCase = true)) RedAlert else BorderColor,
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

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
                            text = "Mínimo 8 caracteres",
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

            // Terms & Conditions Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { acceptedTerms = !acceptedTerms },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = acceptedTerms,
                    onCheckedChange = { acceptedTerms = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = GreenPrimary,
                        uncheckedColor = BorderColor,
                        checkmarkColor = Color.White
                    )
                )
                Text(
                    text = "Acepto los términos y la política de privacidad.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create Account Button
            Button(
                onClick = { onSignUpClick(fullName, email, password) },
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
                    text = "Crear cuenta",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    text = "o",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = TextMuted,
                    fontSize = 13.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = BorderColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Sign Up Button
            OutlinedButton(
                onClick = onGoogleSignUpClick,
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
                        text = "Registrarme con Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¿Ya tienes una cuenta? ",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Inicia sesión",
                    color = GreenPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Protected & Encrypted Footer Text
            Text(
                text = "Tu información está protegida y cifrada.",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BackArrowIcon(
    modifier: Modifier = Modifier,
    tint: Color = TextDark
) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.62f, size.height * 0.25f)
            lineTo(size.width * 0.38f, size.height * 0.50f)
            lineTo(size.width * 0.62f, size.height * 0.75f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = size.width * 0.10f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
