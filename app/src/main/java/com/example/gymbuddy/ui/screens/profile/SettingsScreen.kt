package com.example.gymbuddy.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.gymbuddy.data.model.User
import com.example.gymbuddy.ui.theme.*
import com.example.gymbuddy.ui.viewmodel.SettingsViewModel

// ═══════════════════════════════════════════════════════════════════════
// Ana Ekran
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUser: User?,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    BackHandler { onBack() }

    val uiState       by settingsViewModel.uiState.collectAsState()
    val passwordState by settingsViewModel.passwordState.collectAsState()
    val deleteState   by settingsViewModel.deleteState.collectAsState()

    // Gizlilik state'leri (Firestore'dan başlangıç değerleri)
    var locationVisible by remember(currentUser?.uid) {
        mutableStateOf(currentUser?.locationVisible ?: true)
    }

    // Dialog state'leri
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog   by remember { mutableStateOf(false) }
    var showTimePicker     by remember { mutableStateOf(false) }

    // Şifre dialog başarılı → kapat
    LaunchedEffect(passwordState.isSuccess) {
        if (passwordState.isSuccess) {
            showPasswordDialog = false
            settingsViewModel.clearPasswordState()
        }
    }

    // Hesap silme başarılı → çıkış
    LaunchedEffect(deleteState.isSuccess) {
        if (deleteState.isSuccess) {
            onAccountDeleted()
        }
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            settingsViewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            settingsViewModel.clearMessages()
        }
    }

    // Scrollable time picker dialog
    if (showTimePicker) {
        var pickerHour by remember { mutableIntStateOf(uiState.notifHour) }
        var pickerMin  by remember { mutableIntStateOf(uiState.notifMin) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor   = SurfaceContainer,
            title = {
                Text(
                    "Hatırlatıcı Saati",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Black
                    ),
                    color = OnSurface
                )
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScrollableNumberPicker(
                        value = pickerHour,
                        range = 0..23,
                        onValueChange = { pickerHour = it }
                    )
                    Text(
                        ":",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = LexendFamily, fontWeight = FontWeight.Black
                        ),
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    ScrollableNumberPicker(
                        value = pickerMin,
                        range = 0..59,
                        onValueChange = { pickerMin = it }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    settingsViewModel.setNotifTime(pickerHour, pickerMin)
                    showTimePicker = false
                }) {
                    Text("Tamam", color = Primary,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = LexendFamily, fontWeight = FontWeight.Black))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("İptal", color = OnSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = LexendFamily))
                }
            }
        )
    }

    // Şifre değiştir dialog
    if (showPasswordDialog) {
        ChangePasswordDialog(
            isLoading    = passwordState.isLoading,
            error        = passwordState.error,
            onConfirm    = { current, new -> settingsViewModel.changePassword(current, new) },
            onDismiss    = { showPasswordDialog = false; settingsViewModel.clearPasswordState() }
        )
    }

    // Hesabı sil dialog
    if (showDeleteDialog) {
        DeleteAccountDialog(
            isLoading = deleteState.isLoading,
            error     = deleteState.error,
            onConfirm = { password ->
                currentUser?.uid?.let { settingsViewModel.deleteAccount(password, it) }
            },
            onDismiss = { showDeleteDialog = false; settingsViewModel.clearDeleteState() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background,
        topBar = { SettingsTopBar(onBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // ── Sayfa başlığı ─────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(
                        "UYGULAMA",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = LexendFamily, fontWeight = FontWeight.Black,
                            fontSize = 30.sp, lineHeight = 34.sp
                        ),
                        color = OnSurface
                    )
                    Text(
                        "AYARLARI",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = LexendFamily, fontWeight = FontWeight.Black,
                            fontSize = 30.sp, lineHeight = 34.sp
                        ),
                        color = Primary
                    )
                }
            }

            // ════════════════════════════════════════════════════
            // 1. BİLDİRİMLER
            // ════════════════════════════════════════════════════
            item { SectionHeader(Icons.Outlined.Notifications, "BİLDİRİMLER") }

            item {
                SettingsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // Antrenman hatırlatıcısı
                    ToggleRow(
                        icon     = Icons.Outlined.FitnessCenter,
                        title    = "Antrenman Hatırlatıcısı",
                        subtitle = "Günlük antrenman saatinde hatırlat",
                        checked  = uiState.notifWorkout,
                        onCheckedChange = { settingsViewModel.setNotifWorkout(it) }
                    )

                    // Saat seçici — sadece bildirim açıkken
                    AnimatedVisibility(
                        visible = uiState.notifWorkout,
                        enter   = expandVertically(),
                        exit    = shrinkVertically()
                    ) {
                        Column {
                            SettingsDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        indication        = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { showTimePicker = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment    = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SurfaceContainerHighest),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Schedule,
                                            contentDescription = null,
                                            tint     = Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        "Hatırlatıcı Saati",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = ManropeFamily, fontWeight = FontWeight.Medium
                                        ),
                                        color = OnSurface
                                    )
                                }
                                Text(
                                    "%02d:%02d".format(uiState.notifHour, uiState.notifMin),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = LexendFamily, fontWeight = FontWeight.Black
                                    ),
                                    color = Primary
                                )
                            }
                        }
                    }

                    SettingsDivider()

                    // Seri uyarısı
                    ToggleRow(
                        icon     = Icons.Outlined.LocalFireDepartment,
                        title    = "Seri Uyarısı",
                        subtitle = "Seriniz kırılmak üzereyken uyar",
                        checked  = uiState.notifStreak,
                        onCheckedChange = { settingsViewModel.setNotifStreak(it) }
                    )

                    SettingsDivider()

                    // Arkadaş bildirimleri
                    ToggleRow(
                        icon     = Icons.Outlined.PersonAdd,
                        title    = "Arkadaş Bildirimleri",
                        subtitle = "Biri sizi takip ettiğinde bildir",
                        checked  = uiState.notifFriends,
                        onCheckedChange = { settingsViewModel.setNotifFriends(it) },
                        isLast   = true
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            // ════════════════════════════════════════════════════
            // 2. GİZLİLİK & KONUM
            // ════════════════════════════════════════════════════
            item { SectionHeader(Icons.Outlined.Lock, "GİZLİLİK & KONUM") }

            item {
                SettingsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // Konum paylaşımı
                    ToggleRow(
                        icon     = Icons.Outlined.LocationOn,
                        title    = "Konum Paylaşımı",
                        subtitle = "Connect ekranında diğer üyelere görün",
                        checked  = locationVisible,
                        onCheckedChange = {
                            locationVisible = it
                            settingsViewModel.updatePrivacy("public", it)
                        },
                        isLast = true
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            // ════════════════════════════════════════════════════
            // 3. GÖRÜNÜM
            // ════════════════════════════════════════════════════
            item { SectionHeader(Icons.Outlined.Palette, "GÖRÜNÜM") }

            item {
                SettingsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // Tema
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.DarkMode, null,
                                    tint = Primary, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                "Tema",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold
                                ),
                                color = OnSurface
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "dark"   to "Koyu",
                                "light"  to "Açık",
                                "system" to "Sistem"
                            ).forEach { (value, label) ->
                                val selected = uiState.themeMode == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) Primary else SurfaceContainerHighest)
                                        .clickable(
                                            indication        = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { settingsViewModel.setThemeMode(value) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = LexendFamily, fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = if (selected) OnPrimary else OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    SettingsDivider()

                    // Ölçü birimi
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Scale, null,
                                    tint = Primary, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(
                                    "Ölçü Birimi",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold
                                    ),
                                    color = OnSurface
                                )
                                Text(
                                    "Ağırlık gösterim birimi",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = ManropeFamily
                                    ),
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("kg" to "Kilogram (kg)", "lbs" to "Pound (lbs)").forEach { (value, label) ->
                                val selected = uiState.weightUnit == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) Primary else SurfaceContainerHighest)
                                        .clickable(
                                            indication        = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { settingsViewModel.setWeightUnit(value) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = LexendFamily, fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = if (selected) OnPrimary else OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            // ════════════════════════════════════════════════════
            // 4. HESAP & GÜVENLİK
            // ════════════════════════════════════════════════════
            item { SectionHeader(Icons.Outlined.Security, "HESAP & GÜVENLİK") }

            item {
                SettingsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // Şifre değiştir
                    ClickableRow(
                        icon     = Icons.Outlined.Key,
                        title    = "Şifre Değiştir",
                        subtitle = "Hesap güvenliğini güncelle",
                        onClick  = { showPasswordDialog = true }
                    )

                    SettingsDivider()

                    // Hesabı sil
                    ClickableRow(
                        icon      = Icons.Outlined.DeleteForever,
                        title     = "Hesabı Sil",
                        subtitle  = "Tüm veriler kalıcı olarak silinir",
                        onClick   = { showDeleteDialog = true },
                        isDanger  = true,
                        isLast    = true
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Top Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(Surface.copy(alpha = 0.70f))
            .padding(horizontal = 4.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri",
                tint = Primary, modifier = Modifier.size(24.dp))
        }
        Text(
            "GYMBUDDY",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = LexendFamily, fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp, fontStyle = FontStyle.Italic
            ),
            color    = Primary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Yardımcı composable'lar
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily, fontWeight = FontWeight.Black,
                letterSpacing = 2.sp, fontSize = 10.sp
            ),
            color = Primary
        )
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLow),
        content = content
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        color     = OutlineVariant.copy(alpha = 0.3f),
        thickness = 0.5.dp
    )
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold
                ),
                color = OnSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = ManropeFamily),
                    color = OnSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor       = OnPrimary,
                checkedTrackColor       = Primary,
                uncheckedThumbColor     = OnSurfaceVariant,
                uncheckedTrackColor     = SurfaceContainerHighest,
                uncheckedBorderColor    = OutlineVariant
            )
        )
    }
}

@Composable
private fun ClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    isDanger: Boolean = false,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isDanger) Secondary.copy(alpha = 0.12f)
                    else SurfaceContainerHighest
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null,
                tint     = if (isDanger) Secondary else Primary,
                modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold
                ),
                color = if (isDanger) Secondary else OnSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = ManropeFamily),
                    color = OnSurfaceVariant
                )
            }
        }
        Icon(
            Icons.Outlined.ChevronRight, null,
            tint     = if (isDanger) Secondary.copy(alpha = 0.5f) else OutlineVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Şifre Değiştir Dialog
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ChangePasswordDialog(
    isLoading: Boolean,
    error: String?,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPw  by remember { mutableStateOf("") }
    var newPw      by remember { mutableStateOf("") }
    var confirmPw  by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew     by remember { mutableStateOf(false) }
    var localError  by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceContainer,
        title = {
            Text(
                "Şifre Değiştir",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = LexendFamily, fontWeight = FontWeight.Black
                ),
                color = OnSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Mevcut şifre
                PwField(
                    value         = currentPw,
                    onValueChange = { currentPw = it },
                    label         = "Mevcut Şifre",
                    showPassword  = showCurrent,
                    onToggle      = { showCurrent = !showCurrent }
                )
                // Yeni şifre
                PwField(
                    value         = newPw,
                    onValueChange = { newPw = it },
                    label         = "Yeni Şifre",
                    showPassword  = showNew,
                    onToggle      = { showNew = !showNew }
                )
                // Onay (göz simgesi yok, showNew ile senkron)
                PwField(
                    value         = confirmPw,
                    onValueChange = { confirmPw = it },
                    label         = "Yeni Şifre (Tekrar)",
                    showPassword  = showNew,
                    onToggle      = null
                )
                val displayError = localError ?: error
                if (displayError != null) {
                    Text(
                        displayError,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = ManropeFamily),
                        color = Secondary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        newPw != confirmPw -> localError = "Yeni şifreler eşleşmiyor"
                        newPw.length < 6  -> localError = "Yeni şifre en az 6 karakter olmalı"
                        else -> { localError = null; onConfirm(currentPw, newPw) }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp),
                        color = Primary, strokeWidth = 2.dp)
                } else {
                    Text("Değiştir", color = Primary,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = LexendFamily, fontWeight = FontWeight.Black))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = OnSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = LexendFamily))
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════
// Hesabı Sil Dialog
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DeleteAccountDialog(
    isLoading: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password    by remember { mutableStateOf("") }
    var showPw      by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceContainer,
        title = {
            Text(
                "Hesabı Sil",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = LexendFamily, fontWeight = FontWeight.Black
                ),
                color = Secondary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Bu işlem geri alınamaz. Tüm antrenman geçmişiniz, hedefleriniz ve profil bilgileriniz kalıcı olarak silinecek.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFamily, lineHeight = 20.sp
                    ),
                    color = OnSurfaceVariant
                )
                PwField(
                    value         = password,
                    onValueChange = { password = it },
                    label         = "Şifrenizi Girin",
                    showPassword  = showPw,
                    onToggle      = { showPw = !showPw }
                )
                if (error != null) {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = ManropeFamily),
                        color = Secondary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { onConfirm(password) },
                enabled  = !isLoading && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp),
                        color = Secondary, strokeWidth = 2.dp)
                } else {
                    Text("Hesabı Sil", color = Secondary,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = LexendFamily, fontWeight = FontWeight.Black))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç", color = OnSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = LexendFamily))
            }
        }
    )
}

// ── Şifre input alanı ────────────────────────────────────────────────

@Composable
private fun PwField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPassword: Boolean,
    onToggle: (() -> Unit)? = null   // null → göz simgesi yok
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value           = value,
            onValueChange   = onValueChange,
            singleLine      = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (showPassword) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            textStyle       = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = ManropeFamily, color = OnSurface
            ),
            cursorBrush     = SolidColor(Primary),
            modifier        = Modifier.weight(1f),
            decorationBox   = { inner ->
                if (value.isEmpty()) {
                    Text(label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = ManropeFamily),
                        color = OnSurfaceVariant.copy(alpha = 0.5f))
                }
                inner()
            }
        )
        if (onToggle != null) {
            IconButton(onClick = onToggle, modifier = Modifier.size(20.dp)) {
                Icon(
                    if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Kaydırmalı Sayı Picker
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ScrollableNumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    val items = range.toList()
    val itemHeight = 48.dp
    val visibleItems = 3
    val coroutineScope = rememberCoroutineScope()

    // Editing state — tıklanınca manuel giriş
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(value).coerceAtLeast(0)
    )

    // Scroll bittiğinde değeri güncelle
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex +
                (listState.firstVisibleItemScrollOffset.toFloat() /
                    (itemHeight.value * listState.layoutInfo.viewportSize.height /
                        listState.layoutInfo.viewportSize.height)).toInt()
            val snappedIndex = centerIndex.coerceIn(0, items.lastIndex)
            onValueChange(items[snappedIndex])
        }
    }

    // Dış değer değiştiğinde scroll'u güncelle
    LaunchedEffect(value) {
        val idx = items.indexOf(value)
        if (idx >= 0 && !listState.isScrollInProgress) {
            listState.scrollToItem(idx)
        }
    }

    if (isEditing) {
        // Manuel giriş modu
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        BasicTextField(
            value = editText,
            onValueChange = { newValue ->
                val filtered = newValue.text.filter { it.isDigit() }.take(2)
                editText = TextFieldValue(filtered, TextRange(filtered.length))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                color = Primary,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(Primary),
            modifier = Modifier
                .width(72.dp)
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerHighest)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (!state.isFocused && isEditing) {
                        val parsed = editText.text.toIntOrNull()
                        if (parsed != null && parsed in range) {
                            onValueChange(parsed)
                        }
                        isEditing = false
                    }
                }
        )
    } else {
        // Kaydırmalı mod
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(itemHeight * visibleItems),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                contentPadding = PaddingValues(vertical = itemHeight)
            ) {
                items(items.size) { index ->
                    val num = items[index]
                    val isSelected = num == value
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                val formatted = "%02d".format(num)
                                editText = TextFieldValue(formatted, TextRange(formatted.length))
                                isEditing = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%02d".format(num),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = LexendFamily,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                fontSize = if (isSelected) 28.sp else 18.sp
                            ),
                            color = if (isSelected) Primary else OnSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Seçim göstergesi — ortadaki kutu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary.copy(alpha = 0.08f))
            )
        }
    }
}
