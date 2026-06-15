package com.example.gymbuddy.ui.screens.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
fun LoginScreen(
    onLoginClick: (email: String, password: String) -> Unit = { _, _ -> },
    onGoogleSignInClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onErrorDismiss: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Hata mesajı gösterildikten sonra temizle
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
        // ── Layer 1: Background gradient ────────────────────────────────
        LoginBackgroundLayer()

        // ── Layer 2: Kinetic blur orbs ──────────────────────────────────
        LoginKineticOrbs()

        // ── Layer 3: Main content ───────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp)
                .padding(top = 80.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Brand Header ────────────────────────────────────────────
            LoginBrandHeader()

            Spacer(modifier = Modifier.height(48.dp))

            // ── Email Field ─────────────────────────────────────────────
            LoginInputField(
                value = email,
                onValueChange = { email = it },
                label = "E-posta Adresi",
                placeholder = "name@domain.com",
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

            // ── Password Field ──────────────────────────────────────────
            LoginPasswordField(
                value = password,
                onValueChange = { password = it },
                passwordVisible = passwordVisible,
                onTogglePassword = { passwordVisible = !passwordVisible },
                onForgotPasswordClick = onForgotPasswordClick,
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onLoginClick(email, password)
                    }
                )
            )

            // ── Error Message ───────────────────────────────────────────
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = com.example.gymbuddy.ui.theme.Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Log In Button ───────────────────────────────────────────
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = com.example.gymbuddy.ui.theme.PrimaryContainer,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                LoginButton(
                    onClick = { onLoginClick(email, password) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Social Access Divider ───────────────────────────────────
            SocialDivider()

            Spacer(modifier = Modifier.height(24.dp))

            // ── Google Button ───────────────────────────────────────────
            GoogleSignInButton(
                onClick = onGoogleSignInClick
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Footer: Sign Up link ────────────────────────────────────
            Spacer(modifier = Modifier.height(40.dp))
            FooterSignUpLink(onSignUpClick = onSignUpClick)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Private composable building blocks
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun LoginBackgroundLayer() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A1A),
                            Color(0xFF0E0E0E),
                            Color(0xFF0E0E0E)
                        )
                    )
                )
        )
        // Subtle top light leak
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0f),
                        radius = 700f
                    )
                )
        )
    }
}

@Composable
private fun LoginKineticOrbs() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Bottom-right lime orb
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .blur(120.dp)
                .background(
                    color = Primary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(50)
                )
        )
        // Top-left orange orb
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.TopStart)
                .offset(x = (-70).dp, y = (-40).dp)
                .blur(120.dp)
                .background(
                    color = Secondary.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(50)
                )
        )
    }
}

@Composable
private fun LoginBrandHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with background container (surface-container-high rounded box)
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = Primary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = "GymBuddy Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // GYMBUDDY brand
        Text(
            text = "GYMBUDDY",
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                fontSize = 42.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tagline
        Text(
            text = "YÜKSEK PERFORMANS EDİSYONU",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Label
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        // Input field with leading icon
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0xFF3A3A3A),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = ManropeFamily
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
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
                focusedContainerColor = SurfaceContainerLowest,
                unfocusedContainerColor = SurfaceContainerLowest,
                disabledContainerColor = SurfaceContainerLowest,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        )
    }
}

@Composable
private fun LoginPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Label row with Forgot Password link
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
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "ŞİFREMİ UNUTTUM?",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontSize = 9.sp
                ),
                color = SecondaryDim,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onForgotPasswordClick()
                }
            )
        }

        // Password input
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = "••••••••",
                    color = Color(0xFF3A3A3A),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = ManropeFamily
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.Visibility
                        else Icons.Outlined.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color(0xFF3A3A3A),
                        modifier = Modifier.size(20.dp)
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
                focusedContainerColor = SurfaceContainerLowest,
                unfocusedContainerColor = SurfaceContainerLowest,
                disabledContainerColor = SurfaceContainerLowest,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        )
    }
}

@Composable
private fun LoginButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "loginButtonScale"
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
            defaultElevation = 16.dp,
            pressedElevation = 4.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .scale(scale)
            .drawBehind {
                drawCircle(
                    color = PrimaryContainer.copy(alpha = 0.20f),
                    radius = size.width * 0.5f,
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
                text = "GİRİŞ YAP",
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
private fun SocialDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(OutlineVariant.copy(alpha = 0.20f))
        )
        Text(
            text = "SOSYAL GİRİŞ",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontSize = 9.sp
            ),
            color = Color(0xFF555555),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(OutlineVariant.copy(alpha = 0.20f))
        )
    }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "googleBtnScale"
    )

    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceContainerHigh
        ),
        border = null,
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
    ) {
        // Google "G" text (since we can't load external images)
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(2.dp)
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
            text = "GOOGLE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "KAYDOL",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.primary,
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
