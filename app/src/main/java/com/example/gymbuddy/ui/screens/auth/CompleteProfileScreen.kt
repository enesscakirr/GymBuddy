package com.example.gymbuddy.ui.screens.auth

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.gymbuddy.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Locale

// ── Fitness focus options ───────────────────────────────────────────
enum class FitnessFocus(val label: String) {
    STRENGTH("Strength"),
    CARDIO("Cardio"),
    FLEXIBILITY("Flexibility"),
    ENDURANCE("Endurance")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    onBackClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onSaveAndContinue: (
        fullName: String,
        age: String,
        height: String,
        weight: Float,
        address: String,
        fitnessFocus: String,
        profilePhotoUri: Uri?
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableIntStateOf(80) }
    var address by remember { mutableStateOf("") }
    var selectedFocus by remember { mutableStateOf(setOf(FitnessFocus.STRENGTH)) }
    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }

    // ── Camera URI (temp file for photo capture) ────────────────────
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // ── Photo Picker (Gallery) ──────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profilePhotoUri = it }
    }

    // ── Camera Capture ──────────────────────────────────────────────
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            profilePhotoUri = cameraImageUri
        }
    }

    // ── Camera Permission ───────────────────────────────────────────
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempImageUri(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // ── Location Permission ─────────────────────────────────────────
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            scope.launch {
                isLoadingLocation = true
                address = fetchCurrentAddress(context)
                isLoadingLocation = false
            }
        }
    }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // ── Photo Picker Bottom Sheet ───────────────────────────────────
    if (showPhotoPickerSheet) {
        PhotoPickerBottomSheet(
            onDismiss = { showPhotoPickerSheet = false },
            onGalleryClick = {
                showPhotoPickerSheet = false
                galleryLauncher.launch("image/*")
            },
            onCameraClick = {
                showPhotoPickerSheet = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }

    Scaffold(
        topBar = {
            ProfileTopBar(
                onBackClick = onBackClick,
                onLogoutClick = onLogoutClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Decorative blur orb
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = (-40).dp)
                    .blur(120.dp)
                    .background(
                        color = Primary.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 40.dp)
            ) {
                SectionHeader()
                Spacer(modifier = Modifier.height(28.dp))

                ProfilePicturePicker(
                    photoUri = profilePhotoUri,
                    onClick = { showPhotoPickerSheet = true }
                )
                Spacer(modifier = Modifier.height(32.dp))

                ProfileInputField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Ad Soyad",
                    placeholder = "Ahmet Yılmaz",
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileInputField(
                            value = age,
                            onValueChange = { age = it },
                            label = "Yaş",
                            placeholder = "24",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Right) }
                            )
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileInputField(
                            value = height,
                            onValueChange = { height = it },
                            label = "Boy (cm)",
                            placeholder = "182",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                WeightSliderField(weight = weight, onWeightChange = { weight = it })
                Spacer(modifier = Modifier.height(16.dp))

                AddressField(
                    value = address,
                    onValueChange = { address = it },
                    isLoading = isLoadingLocation,
                    onLocationClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
                Spacer(modifier = Modifier.height(28.dp))

                FitnessFocusSelector(
                    selected = selectedFocus,
                    onToggle = { focus ->
                        selectedFocus = if (focus in selectedFocus) {
                            selectedFocus - focus
                        } else {
                            selectedFocus + focus
                        }
                    }
                )
                // Error message
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

                Spacer(modifier = Modifier.height(36.dp))

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
                    SaveAndContinueButton(
                        onClick = {
                            onSaveAndContinue(
                                fullName, age, height, weight.toFloat(), address,
                                selectedFocus.joinToString(",") { it.name },
                                profilePhotoUri
                            )
                        }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Helper: Create temp file URI for camera
// ═══════════════════════════════════════════════════════════════════════

private fun createTempImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "camera_photos").apply { mkdirs() }
    val file = File.createTempFile("profile_", ".jpg", directory)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

// ═══════════════════════════════════════════════════════════════════════
// Helper: Fetch current address via GPS + Geocoder
// ═══════════════════════════════════════════════════════════════════════

@Suppress("MissingPermission")
private suspend fun fetchCurrentAddress(context: Context): String {
    return try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val location = fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).await()

        if (location != null) {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var result = ""
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val addr = addresses[0]
                        result = buildString {
                            addr.thoroughfare?.let { append("$it, ") }
                            addr.subLocality?.let { append("$it, ") }
                            addr.locality?.let { append("$it, ") }
                            addr.postalCode?.let { append(it) }
                        }.trimEnd(',', ' ')
                    }
                }
                // Small delay for geocoder callback
                kotlinx.coroutines.delay(1000)
                result.ifEmpty { "${location.latitude}, ${location.longitude}" }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    buildString {
                        addr.thoroughfare?.let { append("$it, ") }
                        addr.subLocality?.let { append("$it, ") }
                        addr.locality?.let { append("$it, ") }
                        addr.postalCode?.let { append(it) }
                    }.trimEnd(',', ' ')
                } else {
                    "${location.latitude}, ${location.longitude}"
                }
            }
        } else {
            "Konum alınamadı"
        }
    } catch (e: Exception) {
        "Konum hatası: ${e.localizedMessage}"
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Photo Picker Bottom Sheet
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoPickerBottomSheet(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
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
                text = "PROFIL FOTOĞRAFI",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = Primary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Gallery option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { onGalleryClick() }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = "Galeri",
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Galeriden Seç",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = "Mevcut fotoğraflarından birini seç",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Camera option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { onCameraClick() }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = "Kamera",
                    tint = Secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Fotoğraf Çek",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = "Kameranı kullanarak yeni bir fotoğraf çek",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Building Blocks
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "GYMBUDDY",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            IconButton(onClick = onLogoutClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = "Çıkış",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Surface.copy(alpha = 0.70f),
            scrolledContainerColor = Surface.copy(alpha = 0.90f)
        )
    )
}

@Composable
private fun SectionHeader() {
    Column {
        Text(
            text = "COMPLETE",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                fontSize = 36.sp,
                lineHeight = 40.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "PROFILE",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                fontSize = 36.sp,
                lineHeight = 40.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Profilini tamamla, kişisel deneyimini oluştur.",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfilePicturePicker(
    photoUri: Uri?,
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
                    .size(96.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceContainerHigh)
                    .border(
                        width = 2.dp,
                        color = if (photoUri != null) Primary.copy(alpha = 0.5f)
                        else OutlineVariant.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                if (photoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(photoUri),
                        contentDescription = "Profil fotoğrafı",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Profile",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // Camera badge
            Box(
                modifier = Modifier
                    .offset(x = 6.dp, y = 6.dp)
                    .size(36.dp)
                    .background(
                        color = Primary,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = "Fotoğraf ekle",
                    tint = OnPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (photoUri != null) "FOTOĞRAFI DEĞİŞTİR" else "ADD PROFILE PICTURE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 9.sp
            ),
            color = if (photoUri != null) Primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = ManropeFamily)
                )
            },
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium,
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
private fun WeightSliderField(
    weight: Int,
    onWeightChange: (Int) -> Unit
) {
    Column {
        Text(
            text = "WEIGHT (KG)",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                value = weight.toFloat(),
                onValueChange = { onWeightChange(it.toInt()) },
                valueRange = 30f..200f,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Primary,
                    activeTrackColor = Primary,
                    inactiveTrackColor = SurfaceContainerHigh
                )
            )

            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .background(SurfaceContainerHigh, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$weight KG",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AddressField(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    onLocationClick: () -> Unit
) {
    Column {
        Text(
            text = "ADDRESS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Box {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = "Mahalle, İlçe, Şehir",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = ManropeFamily)
                    )
                },
                minLines = 3,
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium,
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
                modifier = Modifier.fillMaxWidth()
            )

            // Location button (top-right)
            IconButton(
                onClick = onLocationClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = "Konumumu al",
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FitnessFocusSelector(
    selected: Set<FitnessFocus>,
    onToggle: (FitnessFocus) -> Unit
) {
    Column {
        Text(
            text = "PRIMARY FITNESS FOCUS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        // Use FlowRow-style layout with wrapping
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FitnessFocus.entries.take(3).forEach { focus ->
                FitnessChip(
                    label = focus.label,
                    isSelected = focus in selected,
                    onClick = { onToggle(focus) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FitnessFocus.entries.drop(3).forEach { focus ->
                FitnessChip(
                    label = focus.label,
                    isSelected = focus in selected,
                    onClick = { onToggle(focus) }
                )
            }
        }
    }
}

@Composable
private fun FitnessChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Primary else SurfaceContainerHighest,
        animationSpec = tween(200), label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) OnPrimary else OnSurfaceVariant,
        animationSpec = tween(200), label = "chipText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color.Transparent else OutlineVariant.copy(alpha = 0.20f),
        animationSpec = tween(200), label = "chipBorder"
    )

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(50))
            .then(
                if (!isSelected) Modifier.border(1.dp, borderColor, RoundedCornerShape(50))
                else Modifier
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 11.sp
            ),
            color = textColor
        )
    }
}

@Composable
private fun SaveAndContinueButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100), label = "saveBtnScale"
    )

    val limeGradient = Brush.linearGradient(
        colors = listOf(Primary, PrimaryContainer),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .scale(scale)
            .drawBehind {
                drawCircle(
                    color = PrimaryContainer.copy(alpha = 0.25f),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SAVE AND CONTINUE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        fontSize = 16.sp
                    ),
                    color = OnPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = OnPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
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
fun CompleteProfileScreenPreview() {
    GymBuddyTheme {
        CompleteProfileScreen()
    }
}
