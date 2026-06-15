@file:OptIn(ExperimentalLayoutApi::class)

package com.example.gymbuddy.ui.screens.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.gymbuddy.data.repository.DetectedFood
import com.example.gymbuddy.data.repository.FoodAnalysisRepository
import com.example.gymbuddy.data.repository.FoodNutrition
import com.example.gymbuddy.data.repository.MealRepository
import com.example.gymbuddy.data.repository.MealType
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gymbuddy.ui.theme.*

// ── Scan States ─────────────────────────────────────────────────────

private enum class ScanState {
    EMPTY, PREVIEW, SCANNING, ADJUSTING, RESULT
}

// ── Main Screen ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onMenuClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onSaveMeal: (Uri?, List<DetectedFood>?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val foodRepo = remember { FoodAnalysisRepository() }
    val mealRepo = remember { MealRepository() }

    var scanState        by remember { mutableStateOf(ScanState.EMPTY) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var analysisError    by remember { mutableStateOf<String?>(null) }

    // Düzenlenebilir besin listesi
    var adjustedFoods by remember { mutableStateOf<List<DetectedFood>>(emptyList()) }
    // Orijinal besinler (oran hesabı için)
    var originalFoods by remember { mutableStateOf<List<DetectedFood>>(emptyList()) }

    var showSaveSheet by remember { mutableStateOf(false) }
    var isSaving      by remember { mutableStateOf(false) }
    var saveSuccess   by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Fotoğraf seçildiğinde önce preview'a geç
    fun selectPhoto(uri: Uri) {
        selectedImageUri = uri
        scanState = ScanState.PREVIEW
        analysisError = null
        saveSuccess = false
    }

    // Kullanıcı onaylayınca analizi başlat
    fun startAnalysis() {
        val uri = selectedImageUri ?: return
        scanState = ScanState.SCANNING
        scope.launch {
            foodRepo.analyzeFood(context, uri)
                .onSuccess { food ->
                    originalFoods = food.detectedFoods
                    adjustedFoods = food.detectedFoods.toList()
                    scanState = ScanState.ADJUSTING
                }
                .onFailure { e ->
                    analysisError = e.message ?: "Analiz başarısız"
                    scanState = ScanState.PREVIEW
                }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { selectPhoto(it) } }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) pendingCameraUri?.let { selectPhoto(it) } }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createScanTempUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val uri = createScanTempUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gramaj değiştirme — orantılı hesaplama
    fun adjustGrams(index: Int, delta: Int) {
        val list = adjustedFoods.toMutableList()
        val orig = originalFoods[index]
        val current = list[index]
        val newGrams = (current.grams + delta).coerceAtLeast(10)
        val ratio = newGrams.toFloat() / orig.grams.toFloat()

        list[index] = current.copy(
            grams        = newGrams,
            calories     = (orig.calories * ratio).toInt(),
            protein      = (orig.protein * ratio * 10).toInt() / 10f,
            carbs        = (orig.carbs * ratio * 10).toInt() / 10f,
            fat          = (orig.fat * ratio * 10).toInt() / 10f,
            fiber        = (orig.fiber * ratio * 10).toInt() / 10f,
            sugar        = (orig.sugar * ratio * 10).toInt() / 10f,
            saturatedFat = (orig.saturatedFat * ratio * 10).toInt() / 10f,
            sodium       = (orig.sodium * ratio).toInt(),
            cholesterol  = (orig.cholesterol * ratio).toInt(),
            potassium    = (orig.potassium * ratio).toInt(),
        )
        adjustedFoods = list
    }

    // Bottom sheet
    if (showSaveSheet) {
        SaveMealBottomSheet(
            onDismiss = { showSaveSheet = false },
            isSaving  = isSaving,
            defaultMealName = adjustedFoods.joinToString(", ") { it.name },
            onSave    = { mealName, mealType, date ->
                isSaving = true
                scope.launch {
                    mealRepo.saveMeal(
                        imageUri      = selectedImageUri,
                        mealName      = mealName,
                        mealType      = mealType,
                        date          = date,
                        adjustedFoods = adjustedFoods
                    ).onSuccess {
                        isSaving = false
                        showSaveSheet = false
                        saveSuccess = true
                        Toast.makeText(context, "Öğün kaydedildi!", Toast.LENGTH_SHORT).show()
                    }.onFailure { e ->
                        isSaving = false
                        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
                .verticalScroll(rememberScrollState())
        ) {
            ScanTopBar()
            Spacer(modifier = Modifier.height(16.dp))

            ViewfinderSection(
                imageUri  = selectedImageUri,
                scanState = scanState,
                modifier  = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Hata
            if (analysisError != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Secondary.copy(alpha = 0.12f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text  = analysisError ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = ManropeFamily),
                        color = Secondary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            when (scanState) {
                ScanState.EMPTY -> {
                    PhotoSelectionSection(
                        onGalleryClick = { galleryLauncher.launch("image/*") },
                        onCameraClick  = { launchCamera() },
                        modifier       = Modifier.padding(horizontal = 24.dp)
                    )
                }
                ScanState.PREVIEW -> {
                    // Fotoğrafı onayla veya değiştir
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Fotoğrafı kaydırarak besini çerçeveye hizalayın",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = ManropeFamily, fontSize = 12.sp
                            ),
                            color = OnSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Tara butonu
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(Primary, PrimaryContainer)))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { startAnalysis() }
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "TARA",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = LexendFamily, fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = OnPrimary
                            )
                        }

                        // Değiştir butonu
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceContainerLow)
                                    .drawBehind {
                                        drawRoundRect(
                                            OutlineVariant.copy(alpha = 0.20f),
                                            cornerRadius = CornerRadius(14.dp.toPx()),
                                            style = Stroke(width = 1.dp.toPx())
                                        )
                                    }
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { galleryLauncher.launch("image/*") }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.PhotoLibrary, null, tint = Primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "DEĞİŞTİR",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = LexendFamily, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                                        ),
                                        color = Primary
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceContainerLow)
                                    .drawBehind {
                                        drawRoundRect(
                                            OutlineVariant.copy(alpha = 0.20f),
                                            cornerRadius = CornerRadius(14.dp.toPx()),
                                            style = Stroke(width = 1.dp.toPx())
                                        )
                                    }
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { launchCamera() }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CameraAlt, null, tint = Primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "YENİDEN ÇEK",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = LexendFamily, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                                        ),
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }
                }
                ScanState.SCANNING -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = PrimaryContainer, strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "AI ANALİZ EDİYOR...",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = LexendFamily, fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                color = PrimaryContainer
                            )
                        }
                    }
                }
                ScanState.ADJUSTING -> {
                    // Gramaj düzenleme kartı
                    AdjustPortionsCard(
                        foods      = adjustedFoods,
                        onAdjust   = { index, delta -> adjustGrams(index, delta) },
                        onConfirm  = { scanState = ScanState.RESULT },
                        modifier   = Modifier.padding(horizontal = 24.dp)
                    )
                }
                ScanState.RESULT -> {
                    // Detaylı besin değerleri
                    DetailedNutritionCard(
                        foods    = adjustedFoods,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (saveSuccess) {
                        SavedSuccessBadge(modifier = Modifier.padding(horizontal = 24.dp))
                    } else {
                        SaveMealButton(
                            onClick  = { showSaveSheet = true },
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NewScanButton(
                        onClick = {
                            scanState = ScanState.EMPTY
                            selectedImageUri = null
                            adjustedFoods = emptyList()
                            originalFoods = emptyList()
                            analysisError = null
                            saveSuccess = false
                        },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

// ── Top Bar ─────────────────────────────────────────────────────────

@Composable
private fun ScanTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(Surface.copy(alpha = 0.70f))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "GYMBUDDY",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = LexendFamily, fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp, fontStyle = FontStyle.Italic
            ),
            color = Primary
        )
    }
}

// ── Adjust Portions Card ────────────────────────────────────────────

@Composable
private fun AdjustPortionsCard(
    foods: List<DetectedFood>,
    onAdjust: (Int, Int) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(
            initialOffsetY = { it / 4 }, animationSpec = tween(300)
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceContainerLow)
                .drawBehind {
                    drawRoundRect(
                        color = PrimaryContainer.copy(alpha = 0.15f),
                        cornerRadius = CornerRadius(20.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = PrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "TESPİT EDİLEN BESİNLER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = PrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Gramajları düzenleyip onaylayın",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = ManropeFamily, fontSize = 12.sp
                ),
                color = OnSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Food items with +/- controls
            foods.forEachIndexed { index, food ->
                AdjustableFoodRow(
                    food       = food,
                    onMinus    = { onAdjust(index, -10) },
                    onPlus     = { onAdjust(index, 10) }
                )
                if (index < foods.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toplam kalori özeti
            val totalCal = foods.sumOf { it.calories }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOPLAM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ManropeFamily, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = OnSurfaceVariant
                )
                Text(
                    text = "$totalCal kcal",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Black
                    ),
                    color = Primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Onayla butonu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(Primary, PrimaryContainer)))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onConfirm() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ONAYLA VE DEVAM ET",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ),
                    color = OnPrimary
                )
            }
        }
    }
}

@Composable
private fun AdjustableFoodRow(
    food: DetectedFood,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainer)
            .border(1.dp, OutlineVariant.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Besin bilgisi
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFamily, fontWeight = FontWeight.Bold
                ),
                color = OnSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${food.calories} kcal",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = ManropeFamily, fontSize = 11.sp
                ),
                color = OnSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        // +/- kontrolleri
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Eksi butonu
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHighest)
                    .clickable { onMinus() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "-10g",
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Gramaj değeri
            Box(
                modifier = Modifier
                    .widthIn(min = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${food.grams}g",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Black
                    ),
                    color = Primary
                )
            }

            // Artı butonu
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHighest)
                    .clickable { onPlus() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "+10g",
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── Detailed Nutrition Card ─────────────────────────────────────────

@Composable
private fun DetailedNutritionCard(
    foods: List<DetectedFood>,
    modifier: Modifier = Modifier
) {
    val totalCal     = foods.sumOf { it.calories }
    val totalProtein = foods.sumOf { (it.protein * 10).toInt() } / 10f
    val totalCarbs   = foods.sumOf { (it.carbs * 10).toInt() } / 10f
    val totalFat     = foods.sumOf { (it.fat * 10).toInt() } / 10f
    val totalFiber   = foods.sumOf { (it.fiber * 10).toInt() } / 10f
    val totalSugar   = foods.sumOf { (it.sugar * 10).toInt() } / 10f
    val totalSatFat  = foods.sumOf { (it.saturatedFat * 10).toInt() } / 10f
    val totalSodium  = foods.sumOf { it.sodium }
    val totalChol    = foods.sumOf { it.cholesterol }
    val totalPotass  = foods.sumOf { it.potassium }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(
            initialOffsetY = { it / 4 }, animationSpec = tween(400)
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerLow)
                .drawBehind {
                    drawRoundRect(
                        color = OutlineVariant.copy(alpha = 0.10f),
                        cornerRadius = CornerRadius(24.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BESİN",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = LexendFamily, fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = "DEĞERLERİ",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = LexendFamily, fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = OnSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Primary.copy(alpha = 0.10f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "ONAYLANMIŞ\nDEĞERLER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ManropeFamily, fontWeight = FontWeight.Bold,
                            fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp
                        ),
                        color = Primary, textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Makro halkaları (2x2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NutrientCircle(
                    value = "$totalCal", label = "KALORİ",
                    progress = (totalCal / 2500f).coerceIn(0f, 1f),
                    ringColor = Primary, modifier = Modifier.weight(1f)
                )
                NutrientCircle(
                    value = "${totalProtein.toInt()}g", label = "PROTEİN",
                    progress = (totalProtein / 150f).coerceIn(0f, 1f),
                    ringColor = Secondary, modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NutrientCircle(
                    value = "${totalCarbs.toInt()}g", label = "KARBONHİDRAT",
                    progress = (totalCarbs / 300f).coerceIn(0f, 1f),
                    ringColor = Tertiary, modifier = Modifier.weight(1f)
                )
                NutrientCircle(
                    value = "${totalFat.toInt()}g", label = "YAĞ",
                    progress = (totalFat / 65f).coerceIn(0f, 1f),
                    ringColor = OnSurfaceVariant, modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detaylı besin satırları
            Text(
                text = "DETAYLI DEĞERLER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = ManropeFamily, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = OnSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainer)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NutrientDetailRow("Lif",          "${totalFiber}g")
                NutrientDetailRow("Şeker",        "${totalSugar}g")
                NutrientDetailRow("Doymuş Yağ",   "${totalSatFat}g")
                NutrientDetailRow("Sodyum",       "${totalSodium} mg")
                NutrientDetailRow("Kolesterol",   "${totalChol} mg")
                NutrientDetailRow("Potasyum",     "${totalPotass} mg")
            }
        }
    }
}

@Composable
private fun NutrientDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = ManropeFamily, fontWeight = FontWeight.Medium
            ),
            color = OnSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = ManropeFamily, fontWeight = FontWeight.Bold
            ),
            color = OnSurface
        )
    }
}

// ── Viewfinder Section (pan desteğiyle) ─────────────────────────────

@Composable
private fun ViewfinderSection(
    imageUri: Uri?,
    scanState: ScanState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanAnim")

    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "scanLine"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    // Pan state
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var scale   by remember { mutableStateOf(1f) }

    // Yeni fotoğraf seçildiğinde resetle
    LaunchedEffect(imageUri) {
        offsetX = 0f; offsetY = 0f; scale = 1f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        CornerBrackets(
            modifier = Modifier.fillMaxSize(),
            color = Primary, strokeWidth = 4.dp.value
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerLowest)
                .drawBehind {
                    drawRoundRect(
                        color = Primary.copy(alpha = pulseAlpha),
                        cornerRadius = CornerRadius(24.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                // Pan & zoom ile fotoğraf
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(imageUri) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 3f)
                                offsetX += pan.x
                                offsetY += pan.y
                                // Sınırları koru
                                val maxOff = (scale - 1f) * size.width / 2
                                offsetX = offsetX.coerceIn(-maxOff, maxOff)
                                offsetY = offsetY.coerceIn(-maxOff, maxOff)
                            }
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Seçilen yemek fotoğrafı",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.10f))
                )

                if (scanState == ScanState.SCANNING) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (scanLineProgress * 300).dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Primary.copy(alpha = 0.6f),
                                        Primary,
                                        Primary.copy(alpha = 0.6f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = OnSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "YEMEK FOTOĞRAFI\nYÜKLEYİN",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = LexendFamily, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp, lineHeight = 22.sp
                        ),
                        color = OnSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Galeri veya kamera ile fotoğraf ekleyin",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = ManropeFamily),
                        color = OnSurfaceVariant.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── Kamera için geçici URI ──────────────────────────────────────────

private fun createScanTempUri(context: Context): Uri {
    val dir = File(context.cacheDir, "scan_photos").apply { mkdirs() }
    val file = File.createTempFile("scan_", ".jpg", dir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

// ── Corner Brackets ─────────────────────────────────────────────────

@Composable
private fun CornerBrackets(
    modifier: Modifier = Modifier, color: Color, strokeWidth: Float
) {
    Canvas(modifier = modifier) {
        val bracketLen = 32.dp.toPx()
        val sw = strokeWidth.dp.toPx()
        val radius = 12.dp.toPx()

        drawLine(color, Offset(0f, radius), Offset(0f, bracketLen), sw, StrokeCap.Round)
        drawLine(color, Offset(radius, 0f), Offset(bracketLen, 0f), sw, StrokeCap.Round)
        drawArc(color, 180f, 90f, false, Offset.Zero, Size(radius * 2, radius * 2), style = Stroke(sw, cap = StrokeCap.Round))

        drawLine(color, Offset(size.width, radius), Offset(size.width, bracketLen), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width - radius, 0f), Offset(size.width - bracketLen, 0f), sw, StrokeCap.Round)
        drawArc(color, 270f, 90f, false, Offset(size.width - radius * 2, 0f), Size(radius * 2, radius * 2), style = Stroke(sw, cap = StrokeCap.Round))

        drawLine(color, Offset(0f, size.height - radius), Offset(0f, size.height - bracketLen), sw, StrokeCap.Round)
        drawLine(color, Offset(radius, size.height), Offset(bracketLen, size.height), sw, StrokeCap.Round)
        drawArc(color, 90f, 90f, false, Offset(0f, size.height - radius * 2), Size(radius * 2, radius * 2), style = Stroke(sw, cap = StrokeCap.Round))

        drawLine(color, Offset(size.width, size.height - radius), Offset(size.width, size.height - bracketLen), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width - radius, size.height), Offset(size.width - bracketLen, size.height), sw, StrokeCap.Round)
        drawArc(color, 0f, 90f, false, Offset(size.width - radius * 2, size.height - radius * 2), Size(radius * 2, radius * 2), style = Stroke(sw, cap = StrokeCap.Round))
    }
}

// ── Photo Selection ─────────────────────────────────────────────────

@Composable
private fun PhotoSelectionSection(
    onGalleryClick: () -> Unit, onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Primary, PrimaryContainer)))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onCameraClick() }
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.CameraAlt, null, tint = OnPrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "FOTOĞRAF ÇEK",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Black, letterSpacing = 1.sp
                    ),
                    color = OnPrimary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerLow)
                .drawBehind {
                    drawRoundRect(
                        color = OutlineVariant.copy(alpha = 0.20f),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onGalleryClick() }
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.PhotoLibrary, null, tint = Primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "GALERİDEN SEÇ",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Black, letterSpacing = 1.sp
                    ),
                    color = Primary
                )
            }
        }
    }
}

// ── Nutrient Circle Widget ──────────────────────────────────────────

@Composable
private fun NutrientCircle(
    value: String, label: String,
    progress: Float, ringColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress, animationSpec = tween(800, easing = EaseOut),
        label = "progress"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 4.dp.toPx()
                val arcSize = Size(size.width - strokeW, size.height - strokeW)
                val topLeft = Offset(strokeW / 2, strokeW / 2)
                drawArc(SurfaceContainerHighest, -90f, 360f, false, topLeft, arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round))
                drawArc(ringColor, -90f, animatedProgress * 360f, false, topLeft, arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = LexendFamily, fontWeight = FontWeight.Black, fontSize = 14.sp
                ),
                color = ringColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = LexendFamily, fontWeight = FontWeight.Bold,
                fontSize = 10.sp, letterSpacing = 0.5.sp
            ),
            color = OnSurface
        )
    }
}

// ── Save Meal Bottom Sheet ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveMealBottomSheet(
    onDismiss: () -> Unit, isSaving: Boolean,
    defaultMealName: String = "",
    onSave: (String, MealType, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale("tr")).format(Date()) }

    var mealName         by remember { mutableStateOf(defaultMealName) }
    var selectedMealType by remember { mutableStateOf(suggestMealType()) }
    var selectedDate     by remember { mutableStateOf(today) }
    var showDatePicker   by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateStringToMillis(selectedDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = millisToDateString(it) }
                    showDatePicker = false
                }) { Text("Tamam", color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("İptal", color = OnSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = SurfaceContainerHigh)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = SurfaceContainerHigh,
                    titleContentColor = OnSurface, headlineContentColor = OnSurface,
                    weekdayContentColor = OnSurfaceVariant, dayContentColor = OnSurface,
                    selectedDayContainerColor = PrimaryContainer,
                    selectedDayContentColor = OnPrimary,
                    todayContentColor = Primary, todayDateBorderColor = Primary
                )
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = SurfaceContainerHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OutlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "ÖĞÜNÜ KAYDET",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = LexendFamily, fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = OnSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Öğün ismi
            Text("ÖĞÜN İSMİ", style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
            ), color = OnSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = mealName,
                onValueChange = { mealName = it },
                placeholder = {
                    Text(
                        "ör. Öğle yemeği, Tavuklu salata...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = ManropeFamily),
                        color = OnSurfaceVariant.copy(alpha = 0.4f)
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold,
                    color = OnSurface
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryContainer,
                    unfocusedBorderColor = OutlineVariant.copy(alpha = 0.3f),
                    cursorColor = PrimaryContainer,
                    focusedContainerColor = SurfaceContainer,
                    unfocusedContainerColor = SurfaceContainer
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tarih
            Text("TARİH", style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
            ), color = OnSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainer)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        formatDateForDisplay(selectedDate),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold
                        ),
                        color = OnSurface
                    )
                    Icon(Icons.Filled.CalendarToday, "Tarih seç", tint = Primary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Öğün tipi
            Text("ÖĞÜN TİPİ", style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
            ), color = OnSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MealType.values().forEach { type ->
                    val isSelected = type == selectedMealType
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) PrimaryContainer.copy(alpha = 0.15f)
                                else SurfaceContainer
                            )
                            .border(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) PrimaryContainer else OutlineVariant.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedMealType = type }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            mealTypeShortLabel(type),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = ManropeFamily,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 10.sp, letterSpacing = 0.3.sp
                            ),
                            color = if (isSelected) PrimaryContainer else OnSurfaceVariant,
                            textAlign = TextAlign.Center, maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSaving) Brush.linearGradient(listOf(SurfaceContainer, SurfaceContainer))
                        else Brush.horizontalGradient(listOf(Primary, PrimaryContainer))
                    )
                    .clickable(enabled = !isSaving) {
                        onSave(mealName.ifBlank { defaultMealName }, selectedMealType, selectedDate)
                    }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = PrimaryContainer, strokeWidth = 2.dp, modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        "KAYDET",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = LexendFamily, fontWeight = FontWeight.Black, letterSpacing = 1.sp
                        ),
                        color = OnPrimary
                    )
                }
            }
        }
    }
}

// ── Saved Success Badge ─────────────────────────────────────────────

@Composable
private fun SavedSuccessBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PrimaryContainer.copy(alpha = 0.12f))
            .border(1.dp, PrimaryContainer.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Check, null, tint = PrimaryContainer, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "ÖĞÜN KAYDEDİLDİ",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = LexendFamily, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                ),
                color = PrimaryContainer
            )
        }
    }
}

// ── New Scan Button ─────────────────────────────────────────────────

@Composable
private fun NewScanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .drawBehind {
                drawRoundRect(
                    OutlineVariant.copy(alpha = 0.20f),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CameraAlt, null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "YENİ TARAMA",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = LexendFamily, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                ),
                color = Primary
            )
        }
    }
}

// ── Save Meal Button ────────────────────────────────────────────────

@Composable
private fun SaveMealButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRoundRect(
                    Primary.copy(alpha = 0.15f),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    size = Size(size.width, size.height + 8.dp.toPx()),
                    topLeft = Offset(0f, 4.dp.toPx())
                )
            }
            .background(Brush.horizontalGradient(listOf(Primary, PrimaryContainer)))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "ÖĞÜNÜ KAYDET",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = LexendFamily, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp
            ),
            color = OnPrimary
        )
    }
}

// ── Yardımcı fonksiyonlar ───────────────────────────────────────────

private fun suggestMealType(): MealType {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..10  -> MealType.BREAKFAST
        hour in 11..14 -> MealType.LUNCH
        hour in 15..17 -> MealType.SNACK
        else           -> MealType.DINNER
    }
}

private fun mealTypeShortLabel(type: MealType) = when (type) {
    MealType.BREAKFAST -> "Kahvaltı"
    MealType.LUNCH     -> "Öğle"
    MealType.DINNER    -> "Akşam"
    MealType.SNACK     -> "Ara"
}

private fun formatDateForDisplay(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("tr"))
        val date = sdf.parse(dateStr) ?: return dateStr
        val today = sdf.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 2)
        val tomorrow = sdf.format(cal.time)
        val displaySdf = SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr"))
        val formatted = displaySdf.format(date)
        when (dateStr) {
            today     -> "Bugün — $formatted"
            yesterday -> "Dün — $formatted"
            tomorrow  -> "Yarın — $formatted"
            else      -> formatted
        }
    } catch (e: Exception) { dateStr }
}

private fun dateStringToMillis(dateStr: String): Long {
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale("tr")).parse(dateStr)?.time
            ?: System.currentTimeMillis()
    } catch (e: Exception) { System.currentTimeMillis() }
}

private fun millisToDateString(millis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale("tr")).format(Date(millis))
}
