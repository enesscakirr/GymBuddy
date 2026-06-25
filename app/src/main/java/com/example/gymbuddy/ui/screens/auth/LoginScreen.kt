package com.example.gymbuddy.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
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
fun LoginScreen(
    onLoginClick: (email: String, password: String) -> Unit = { _, _ -> },
    onGoogleSignInClick: () -> Unit = {},
    onForgotPasswordClick: (email: String) -> Unit = {},
    onSignUpClick: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onErrorDismiss: () -> Unit = {},
    resetPasswordMessage: String? = null,
    onResetPasswordDismiss: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(4000)
            onErrorDismiss()
        }
    }

    LaunchedEffect(resetPasswordMessage) {
        if (resetPasswordMessage != null) {
            kotlinx.coroutines.delay(4000)
            onResetPasswordDismiss()
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            onDismiss = { showForgotPasswordDialog = false },
            onSend = { resetEmail ->
                onForgotPasswordClick(resetEmail)
                showForgotPasswordDialog = false
            }
        )
    }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Layer 1: Background gradient ────────────────────────────────
        LoginBackgroundLayer()

        // ── Layer 2: Aurora glow orbs ───────────────────────────────────
        LoginAuroraOrbs()

        // ── Layer 3: Main content ───────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 72.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Brand Header ────────────────────────────────────────────
            LoginBrandHeader()

            Spacer(modifier = Modifier.height(40.dp))

            // ── Glass Card ──────────────────────────────────────────────
            GlassCard {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    // ── Email Field ─────────────────────────────────────
                    PremiumInputField(
                        value = email,
                        onValueChange = { email = it },
                        label = "E-POSTA ADRESİ",
                        placeholder = "ornek@eposta.com",
                        leadingIcon = Icons.Outlined.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Password Field ──────────────────────────────────
                    PremiumPasswordField(
                        value = password,
                        onValueChange = { password = it },
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        onForgotPasswordClick = { showForgotPasswordDialog = true },
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onLoginClick(email, password)
                            }
                        )
                    )

                    // ── Error Message ───────────────────────────────────
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage,
                            color = Error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    // ── Reset Password Message ─────────────────────────
                    if (resetPasswordMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = resetPasswordMessage,
                            color = if (resetPasswordMessage.contains("gönderildi")) Primary else Error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Log In Button ───────────────────────────────────
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
                        ShimmerButton(
                            text = "GİRİŞ YAP",
                            onClick = { onLoginClick(email, password) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Social Divider ──────────────────────────────────────────
            SocialDivider()

            Spacer(modifier = Modifier.height(20.dp))

            // ── Google Button ───────────────────────────────────────────
            GoogleSignInButton(onClick = onGoogleSignInClick)

            Spacer(modifier = Modifier.weight(1f))

            // ── Footer ─────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(40.dp))
            FooterSignUpLink(onSignUpClick = onSignUpClick)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Premium Building Blocks
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun LoginBackgroundLayer() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Base dark gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF151515),
                            Color(0xFF0E0E0E),
                            Color(0xFF0A0A0A)
                        )
                    )
                )
        )
        // Subtle lime tint — top area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Subtle orange tint — bottom right corner
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .fillMaxHeight(0.3f)
                .align(Alignment.BottomEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Secondary.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun LoginAuroraOrbs() {
    // No orbs — clean gradient background only
}

@Composable
private fun LoginBrandHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing icon container
        Box(
            modifier = Modifier
                .size(68.dp)
                .drawBehind {
                    // Outer glow
                    drawCircle(
                        color = Primary.copy(alpha = 0.12f),
                        radius = size.width * 0.8f
                    )
                }
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.15f),
                            PrimaryContainer.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 0.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = "GymBuddy Logo",
                tint = Primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "GYMBUDDY",
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                fontSize = 42.sp
            ),
            color = Primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Gradient accent line
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(2.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Primary, Secondary)
                    ),
                    shape = RoundedCornerShape(1.dp)
                )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "YÜKSEK PERFORMANS EDİSYONU",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                fontSize = 10.sp
            ),
            color = OnSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// ── Glass Card ──────────────────────────────────────────────────────────

@Composable
private fun GlassCard(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                color = Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.03f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        content()
    }
}

// ── Premium Input Field ─────────────────────────────────────────────────

@Composable
private fun PremiumInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 10.sp
            ),
            color = OnSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0xFF2A2A2A),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = ManropeFamily
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = OnSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = ManropeFamily,
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Black.copy(alpha = 0.5f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                disabledContainerColor = Color.Black.copy(alpha = 0.3f),
                cursorColor = Primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedLeadingIconColor = Primary,
                unfocusedLeadingIconColor = OnSurfaceVariant.copy(alpha = 0.5f),
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(14.dp)
                )
        )
    }
}

// ── Premium Password Field ──────────────────────────────────────────────

@Composable
private fun PremiumPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ŞİFRE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 10.sp
                ),
                color = OnSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "Unuttum?",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = Secondary,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onForgotPasswordClick()
                }
            )
        }

        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = "••••••••",
                    color = Color(0xFF2A2A2A),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = ManropeFamily
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = OnSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.Visibility
                        else Icons.Outlined.VisibilityOff,
                        contentDescription = if (passwordVisible) "Gizle" else "Göster",
                        tint = Color(0xFF3A3A3A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = keyboardActions,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = ManropeFamily,
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Black.copy(alpha = 0.5f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                disabledContainerColor = Color.Black.copy(alpha = 0.3f),
                cursorColor = Primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedLeadingIconColor = Primary,
                unfocusedLeadingIconColor = OnSurfaceVariant.copy(alpha = 0.5f),
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(14.dp)
                )
        )
    }
}

// ── Shimmer Button ──────────────────────────────────────────────────────

@Composable
private fun ShimmerButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "btnScale"
    )

    // Shimmer animation
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val limeGradient = Brush.linearGradient(
        colors = listOf(Primary, PrimaryContainer),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(limeGradient, RoundedCornerShape(16.dp))
                .drawWithContent {
                    drawContent()
                    // Shimmer overlay
                    val shimmerWidth = size.width * 0.4f
                    val startX = shimmerOffset * size.width
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            start = Offset(startX, 0f),
                            end = Offset(startX + shimmerWidth, size.height)
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    fontSize = 16.sp
                ),
                color = OnPrimary
            )
        }
    }
}

// ── Social Divider ──────────────────────────────────────────────────────

@Composable
private fun SocialDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, OutlineVariant.copy(alpha = 0.3f))
                    )
                )
        )
        Text(
            text = "SOSYAL GİRİŞ",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontSize = 9.sp
            ),
            color = OnSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(OutlineVariant.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )
    }
}

// ── Google Sign-In Button ───────────────────────────────────────────────

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "googleBtnScale"
    )

    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = 0.04f)
        ),
        border = BorderStroke(
            width = 0.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.03f)
                )
            )
        ),
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "G",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = Color(0xFF4285F4)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Google ile devam et",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

// ── Footer ──────────────────────────────────────────────────────────────

@Composable
private fun FooterSignUpLink(onSignUpClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Hesabın yok mu?",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium
            ),
            color = OnSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Kaydol",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = Primary,
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onSignUpClick()
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Forgot Password Dialog
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ForgotPasswordDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var resetEmail by remember { mutableStateOf(initialEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Şifre Sıfırla",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = "E-posta adresine şifre sıfırlama bağlantısı göndereceğiz.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFamily
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    placeholder = {
                        Text(
                            text = "ornek@eposta.com",
                            color = Color(0xFF3A3A3A),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = ManropeFamily
                            )
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = ManropeFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceContainerLowest,
                        unfocusedContainerColor = SurfaceContainerLowest,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(14.dp)
                        )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSend(resetEmail) }) {
                Text(
                    text = "GÖNDER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = Primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "İPTAL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
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
fun LoginScreenPreview() {
    GymBuddyTheme {
        LoginScreen()
    }
}
