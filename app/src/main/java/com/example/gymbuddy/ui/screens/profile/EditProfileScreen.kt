package com.example.gymbuddy.ui.screens.profile

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.gymbuddy.data.model.User
import com.example.gymbuddy.ui.screens.auth.FitnessFocus
import com.example.gymbuddy.ui.theme.*
import com.example.gymbuddy.ui.viewmodel.AuthViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Locale

// ── Fitness focus Türkçe etiketleri ────────────────────────────────

private val focusTrLabels = mapOf(
    FitnessFocus.STRENGTH    to "Güç Antrenmanı",
    FitnessFocus.CARDIO      to "Kardio & HIIT",
    FitnessFocus.FLEXIBILITY to "Esneklik",
    FitnessFocus.ENDURANCE   to "Dayanıklılık"
)

// ── Main Screen ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    currentUser: User?,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val editState by authViewModel.editProfileState.collectAsState()

    // ── Form state — pre-fill mevcut kullanıcı verisinden ──────────
    var fullName  by remember { mutableStateOf(currentUser?.fullName  ?: "") }
    var username  by remember { mutableStateOf(currentUser?.username  ?: "") }
    var age       by remember { mutableStateOf(currentUser?.age       ?: "") }
    var height    by remember { mutableStateOf(currentUser?.height    ?: "") }
    var weight    by remember { mutableIntStateOf((currentUser?.weight ?: 70f).toInt()) }
    var address   by remember { mutableStateOf(currentUser?.address   ?: "") }

    val initialFocus = remember {
        currentUser?.fitnessFocus
            ?.split(",")
            ?.mapNotNull { s -> FitnessFocus.entries.find { it.name.equals(s.trim(), true) } }
            ?.toSet()
            ?.ifEmpty { null }
            ?: setOf(FitnessFocus.STRENGTH)
    }
    var selectedFocus by remember { mutableStateOf(initialFocus) }

    // ── Fotoğraf state ──────────────────────────────────────────────
    var newPhotoUri        by remember { mutableStateOf<Uri?>(null) }
    var cameraImageUri     by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSheet     by remember { mutableStateOf(false) }
    var isLoadingLocation  by remember { mutableStateOf(false) }

    // Galeriden seç
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { newPhotoUri = it } }

    // Kamera ile çek
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) newPhotoUri = cameraImageUri }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createEditTempImageUri(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val ok = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                 perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            scope.launch {
                isLoadingLocation = true
                address = fetchEditAddress(context)
                isLoadingLocation = false
            }
        }
    }

    // Kayıt başarılı → geri dön
    LaunchedEffect(editState.isSuccess) {
        if (editState.isSuccess) {
            authViewModel.clearEditProfileState()
            onBack()
        }
    }

    BackHandler { onBack() }

    // ── Fotoğraf seçici bottom sheet ────────────────────────────────
    if (showPhotoSheet) {
        EditPhotoPickerSheet(
            onDismiss      = { showPhotoSheet = false },
            onGalleryClick = { showPhotoSheet = false; galleryLauncher.launch("image/*") },
            onCameraClick  = { showPhotoSheet = false; cameraPermLauncher.launch(Manifest.permission.CAMERA) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ──────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text(
                    text = "PROFİLİ",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        lineHeight = 36.sp
                    ),
                    color = OnSurface
                )
                Text(
                    text = "DÜZENLE",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        lineHeight = 36.sp
                    ),
                    color = Primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bilgilerini dilediğin zaman güncelleyebilirsin.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = OnSurfaceVariant
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Profil Fotoğrafı ─────────────────────────────────
                EditPhotoPicker(
                    newUri      = newPhotoUri,
                    existingUrl = currentUser?.profilePhotoUrl ?: "",
                    initials    = currentUser?.let {
                        it.fullName.split(" ")
                            .filter { s -> s.isNotBlank() }
                            .take(2)
                            .joinToString("") { s -> s.first().uppercaseChar().toString() }
                    } ?: "?",
                    onClick = { showPhotoSheet = true }
                )

                // ── Ad Soyad ─────────────────────────────────────────
                EditInputField(
                    value          = fullName,
                    onValueChange  = { fullName = it },
                    label          = "Ad Soyad",
                    placeholder    = "Adını gir",
                    trailingIcon   = {
                        Icon(Icons.Outlined.Badge, null,
                            tint = Outline, modifier = Modifier.size(20.dp))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction    = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                // ── Kullanıcı Adı ────────────────────────────────────
                EditInputField(
                    value          = username,
                    onValueChange  = { username = it },
                    label          = "Kullanıcı Adı",
                    placeholder    = "@kullanici_adi",
                    trailingIcon   = {
                        Icon(Icons.Outlined.AlternateEmail, null,
                            tint = Outline, modifier = Modifier.size(20.dp))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction    = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                // ── Yaş + Boy (yan yana) ─────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditInputField(
                            value          = age,
                            onValueChange  = { age = it },
                            label          = "Yaş",
                            placeholder    = "24",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction    = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Right) }
                            )
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditInputField(
                            value          = height,
                            onValueChange  = { height = it },
                            label          = "Boy (cm)",
                            placeholder    = "182",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction    = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )
                    }
                }

                // ── Kilo Slider ──────────────────────────────────────
                EditWeightSlider(weight = weight, onWeightChange = { weight = it })

                // ── Adres ────────────────────────────────────────────
                EditAddressField(
                    value         = address,
                    onValueChange = { address = it },
                    isLoading     = isLoadingLocation,
                    onLocationClick = {
                        locationPermLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )

                // ── Fitness Odağı ────────────────────────────────────
                EditFocusSelector(
                    selected = selectedFocus,
                    onToggle = { focus ->
                        selectedFocus = if (focus in selectedFocus)
                            selectedFocus - focus else selectedFocus + focus
                    }
                )

                // ── Hata mesajı ──────────────────────────────────────
                if (editState.error != null) {
                    Text(
                        text = editState.error!!,
                        color = Error,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = ManropeFamily),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Kaydet butonu ────────────────────────────────────
                SaveButton(
                    isLoading = editState.isSaving,
                    onClick   = {
                        authViewModel.updateProfile(
                            fullName     = fullName.trim(),
                            username     = username.trim().let {
                                if (it.startsWith("@")) it else "@$it"
                            },
                            age          = age.trim(),
                            height       = height.trim(),
                            weight       = weight.toFloat(),
                            address      = address.trim(),
                            fitnessFocus = selectedFocus.joinToString(",") { it.name },
                            photoUri     = newPhotoUri
                        )
                    }
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // ── Sabit Top Bar ───────────────────────────────────────────
        EditProfileTopBar(onBack = onBack)
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Top Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EditProfileTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(Surface.copy(alpha = 0.70f))
            .padding(horizontal = 4.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Geri",
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = "GYMBUDDY",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                fontStyle = FontStyle.Italic
            ),
            color = Primary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Fotoğraf Seçici
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EditPhotoPicker(
    newUri: Uri?,
    existingUrl: String,
    initials: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceContainerHigh)
                    .border(
                        width = 2.dp,
                        color = if (newUri != null || existingUrl.isNotBlank())
                            Primary.copy(alpha = 0.5f) else OutlineVariant.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onClick() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    newUri != null -> Image(
                        painter = rememberAsyncImagePainter(newUri),
                        contentDescription = "Yeni fotoğraf",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))
                    )
                    existingUrl.isNotBlank() -> AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(existingUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profil fotoğrafı",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 36.sp
                        ),
                        color = OnSurfaceVariant
                    )
                }
            }

            // Kamera badge
            Box(
                modifier = Modifier
                    .offset(x = 6.dp, y = 6.dp)
                    .size(34.dp)
                    .background(Primary, RoundedCornerShape(10.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CameraAlt, null, tint = OnPrimary, modifier = Modifier.size(17.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (newUri != null) "FOTOĞRAFI DEĞİŞTİR" else "FOTOĞRAF EKLE / DEĞİŞTİR",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontSize = 9.sp
            ),
            color = if (newUri != null) Primary else OnSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Input Field
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EditInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontSize = 10.sp
            ),
            color = OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        TextField(
            value          = value,
            onValueChange  = onValueChange,
            placeholder    = {
                Text(
                    placeholder,
                    color = Outline,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = ManropeFamily)
                )
            },
            trailingIcon   = trailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine     = true,
            textStyle      = MaterialTheme.typography.bodyLarge.copy(
                fontFamily  = ManropeFamily,
                fontWeight  = FontWeight.Medium,
                color       = OnSurface
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = SurfaceContainerLowest,
                unfocusedContainerColor = SurfaceContainerLowest,
                cursorColor             = Primary,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor  = Color.Transparent
            ),
            shape    = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(58.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Kilo Slider
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EditWeightSlider(weight: Int, onWeightChange: (Int) -> Unit) {
    Column {
        Text(
            text = "KİLO (KG)",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontSize = 10.sp
            ),
            color = OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value          = weight.toFloat(),
                onValueChange  = { onWeightChange(it.toInt()) },
                valueRange     = 30f..200f,
                modifier       = Modifier.weight(1f).padding(start = 8.dp),
                colors         = SliderDefaults.colors(
                    thumbColor        = Primary,
                    activeTrackColor  = Primary,
                    inactiveTrackColor = SurfaceContainerHigh
                )
            )
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .background(SurfaceContainerHigh, RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "$weight KG",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        fontSize   = 14.sp
                    ),
                    color = Primary
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Adres alanı
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EditAddressField(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    onLocationClick: () -> Unit
) {
    Column {
        Text(
            text = "ADRES",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontSize = 10.sp
            ),
            color = OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Box {
            TextField(
                value         = value,
                onValueChange = onValueChange,
                placeholder   = {
                    Text(
                        "Mahalle, İlçe, Şehir",
                        color = Outline,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = ManropeFamily)
                    )
                },
                minLines  = 3,
                maxLines  = 4,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium,
                    color      = OnSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = SurfaceContainerLowest,
                    unfocusedContainerColor = SurfaceContainerLowest,
                    cursorColor             = Primary,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor  = Color.Transparent
                ),
                shape    = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            IconButton(
                onClick  = onLocationClick,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color    = Primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Outlined.MyLocation,
                        contentDescription = "Konumumu al",
                        tint     = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Fitness odak seçici
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EditFocusSelector(
    selected: Set<FitnessFocus>,
    onToggle: (FitnessFocus) -> Unit
) {
    Column {
        Text(
            text = "FİTNESS ODAĞI",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontSize = 10.sp
            ),
            color = OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FitnessFocus.entries.take(2).forEach { f ->
                EditFocusChip(
                    label      = focusTrLabels[f] ?: f.label,
                    isSelected = f in selected,
                    onClick    = { onToggle(f) },
                    modifier   = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FitnessFocus.entries.drop(2).forEach { f ->
                EditFocusChip(
                    label      = focusTrLabels[f] ?: f.label,
                    isSelected = f in selected,
                    onClick    = { onToggle(f) },
                    modifier   = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EditFocusChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg   by animateColorAsState(if (isSelected) Primary else SurfaceContainerHighest, tween(200), label = "bg")
    val text by animateColorAsState(if (isSelected) OnPrimary else OnSurfaceVariant, tween(200), label = "tx")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily    = ManropeFamily,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontSize      = 11.sp
            ),
            color = text
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Kaydet butonu
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SaveButton(isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = !isLoading,
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        modifier = Modifier.fillMaxWidth().height(58.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(listOf(Primary, PrimaryContainer)),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color       = OnPrimaryContainer,
                    strokeWidth = 2.5.dp,
                    modifier    = Modifier.size(26.dp)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint     = OnPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text  = "KAYDET",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily    = LexendFamily,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 3.sp,
                            fontSize      = 15.sp
                        ),
                        color = OnPrimaryContainer
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Photo Picker Bottom Sheet
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPhotoPickerSheet(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceContainer,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(40.dp).height(4.dp)
                    .background(OutlineVariant, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 36.dp)
        ) {
            Text(
                text = "PROFİL FOTOĞRAFI",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = Primary,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { onGalleryClick() }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.PhotoLibrary, null, tint = Primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Galeriden Seç",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ManropeFamily, fontWeight = FontWeight.Bold
                        ),
                        color = OnSurface
                    )
                    Text(
                        "Mevcut fotoğraflarından birini seç",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { onCameraClick() }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.CameraAlt, null, tint = Secondary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Fotoğraf Çek",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ManropeFamily, fontWeight = FontWeight.Bold
                        ),
                        color = OnSurface
                    )
                    Text(
                        "Kameranı kullanarak yeni bir fotoğraf çek",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Yardımcı fonksiyonlar
// ═══════════════════════════════════════════════════════════════════════

private fun createEditTempImageUri(context: Context): Uri {
    val dir  = File(context.cacheDir, "camera_photos").apply { mkdirs() }
    val file = File.createTempFile("edit_profile_", ".jpg", dir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Suppress("MissingPermission")
private suspend fun fetchEditAddress(context: Context): String {
    return try {
        val client   = LocationServices.getFusedLocationProviderClient(context)
        val location = client.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).await() ?: return "Konum alınamadı"

        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            var result = ""
            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                if (addresses.isNotEmpty()) {
                    val a = addresses[0]
                    result = buildString {
                        a.thoroughfare?.let { append("$it, ") }
                        a.subLocality?.let  { append("$it, ") }
                        a.locality?.let     { append("$it, ") }
                        a.postalCode?.let   { append(it) }
                    }.trimEnd(',', ' ')
                }
            }
            kotlinx.coroutines.delay(1000)
            result.ifEmpty { "${location.latitude}, ${location.longitude}" }
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val a = addresses[0]
                buildString {
                    a.thoroughfare?.let { append("$it, ") }
                    a.subLocality?.let  { append("$it, ") }
                    a.locality?.let     { append("$it, ") }
                    a.postalCode?.let   { append(it) }
                }.trimEnd(',', ' ')
            } else "${location.latitude}, ${location.longitude}"
        }
    } catch (e: Exception) {
        "Konum hatası: ${e.localizedMessage}"
    }
}
