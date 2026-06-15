package com.example.gymbuddy.ui.screens.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymbuddy.ui.theme.*

@Composable
fun RegisterScreen(
    onRegisterClick: (fullName: String, email: String, password: String) -> Unit = { _, _, _ -> },
    onLoginClick: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onErrorDismiss: () -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(4000)
            onErrorDismiss()
        }
    }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Layer 1: Background gradient (simulates gym photo scrim) ────
        BackgroundLayer()

        // ── Layer 2: Decorative kinetic blur orbs ───────────────────────
        KineticOrbs()

        // ── Layer 3: Main content ───────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Brand Header ────────────────────────────────────────────
            BrandHeader()

            Spacer(modifier = Modifier.height(40.dp))

            // ── Title Section ───────────────────────────────────────────
            TitleSection()

            Spacer(modifier = Modifier.height(36.dp))

            // ── Form Fields ─────────────────────────────────────────────
            GymInputField(
                value = fullName,
                onValueChange = { fullName = it },
                label = "Ad Soyad",
                placeholder = "Ahmet Yılmaz",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            GymInputField(
                value = email,
                onValueChange = { email = it },
                label = "E-posta Adresi",
                placeholder = "ornek@domain.com",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            GymInputField(
                value = password,
                onValueChange = { password = it },
                label = "Şifre",
                placeholder = "••••••••",
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePassword = { passwordVisible = !passwordVisible },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onRegisterClick(fullName, email, password)
                    }
                )
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ── Error Message ────────────────────────────────────────────
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Register Button (Primary CTA with lime gradient) ────────
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PrimaryContainer,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                RegisterButton(
                    onClick = { onRegisterClick(fullName, email, password) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Footer: Login link ──────────────────────────────────────
            Spacer(modifier = Modifier.height(40.dp))
            FooterLoginLink(onLoginClick = onLoginClick)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Private composable building blocks
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun BackgroundLayer() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Dark gradient that simulates a gym background with scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A1A),
                            Color(0xFF0E0E0E),
                            Color(0xFF0E0E0E)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        // Subtle top-center light leak
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0f),
                        radius = 600f
                    )
                )
        )
    }
}

@Composable
private fun KineticOrbs() {
    // Bottom-right lime orb
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .blur(120.dp)
                .background(
                    color = Primary.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(50)
                )
        )
        // Top-left orange orb
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopStart)
                .offset(x = (-60).dp, y = (-60).dp)
                .blur(120.dp)
                .background(
                    color = Secondary.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(50)
                )
        )
    }
}

@Composable
private fun BrandHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dumbbell icon
        Icon(
            imageVector = Icons.Outlined.FitnessCenter,
            contentDescription = "GymBuddy Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // GYMBUDDY brand name
        Text(
            text = "GYMBUDDY",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TitleSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hesap Oluştur",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Yüksek performanslı sporcuların dünyasına katıl.",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GymInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Label — Lexend uppercase tracking
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Input field — deep black well
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = ManropeFamily
                    )
                )
            },
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { onTogglePassword?.invoke() }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Outlined.Visibility
                            } else {
                                Icons.Outlined.VisibilityOff
                            },
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else null,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = ManropeFamily,
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceContainerLowest,
                unfocusedContainerColor = SurfaceContainerLowest,
                disabledContainerColor = SurfaceContainerLowest,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        )
    }
}

@Composable
private fun RegisterButton(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "buttonScale"
    )

    // Lime gradient: primary → primaryContainer at 135°
    val limeGradient = Brush.linearGradient(
        colors = listOf(Primary, PrimaryContainer),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale)
            // Glow shadow
            .drawBehind {
                drawCircle(
                    color = PrimaryContainer.copy(alpha = 0.25f),
                    radius = size.width * 0.6f,
                    center = Offset(size.width / 2, size.height + 10f)
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(limeGradient, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "KAYDOL",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    fontSize = 17.sp
                ),
                color = OnPrimary
            )
        }
    }
}

@Composable
private fun FooterLoginLink(
    onLoginClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Zaten hesabın var mı?",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Giriş Yap",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onLoginClick()
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Preview
// ═══════════════════════════════════════════════════════════════════════

@Preview(
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFF0E0E0E
)
@Composable
fun RegisterScreenPreview() {
    GymBuddyTheme {
        RegisterScreen()
    }
}
