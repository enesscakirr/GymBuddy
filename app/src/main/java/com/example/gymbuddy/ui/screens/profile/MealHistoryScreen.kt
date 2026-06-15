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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.gymbuddy.data.repository.MealEntry
import com.example.gymbuddy.data.repository.MealRepository
import com.example.gymbuddy.data.repository.MealType
import com.example.gymbuddy.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════
// MealHistoryScreen — Öğün Geçmişi
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun MealHistoryScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val repo = remember { MealRepository() }
    var allMeals by remember { mutableStateOf<List<MealEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var expandedDate by remember { mutableStateOf<String?>(null) }
    var expandedMealId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repo.getAllMeals()
            .onSuccess { allMeals = it }
            .onFailure { e -> android.util.Log.e("MealHistory", "Yükleme hatası", e) }
        isLoading = false
    }

    // Tarihe göre grupla
    val groupedMeals = remember(allMeals) {
        allMeals.groupBy { it.date }
            .toSortedMap(compareByDescending { it })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val topBarHeight = statusBarTop + 56.dp

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp, top = topBarHeight + 8.dp)
            ) {
                // ── Sayfa başlığı ───────────────────────────────
                item {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
                        Text(
                            text = "ÖĞÜN",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = LexendFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 30.sp,
                                lineHeight = 34.sp
                            ),
                            color = OnSurface
                        )
                        Text(
                            text = "GEÇMİŞİM",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = LexendFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 30.sp,
                                lineHeight = 34.sp
                            ),
                            color = Primary
                        )
                    }
                }

                if (groupedMeals.isEmpty()) {
                    item { EmptyMealsCard() }
                } else {
                    // ── Her tarih için bir kart ─────────────────
                    groupedMeals.forEach { (date, meals) ->
                        val isExpanded = expandedDate == date
                        val totalCal = meals.sumOf { it.calories }

                        item(key = "date_$date") {
                            DayCard(
                                date = date,
                                mealCount = meals.size,
                                totalCalories = totalCal,
                                isExpanded = isExpanded,
                                onClick = {
                                    expandedDate = if (isExpanded) null else date
                                    expandedMealId = null
                                }
                            )
                        }

                        // Açıldığında o günün öğünleri
                        if (isExpanded) {
                            items(meals, key = { it.id }) { meal ->
                                val isMealExpanded = expandedMealId == meal.id
                                MealCard(
                                    meal = meal,
                                    isExpanded = isMealExpanded,
                                    onClick = {
                                        expandedMealId = if (isMealExpanded) null else meal.id
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Top Bar ─────────────────────────────────────────────
        MealHistoryTopBar(onBack = onBack)
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Top Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun MealHistoryTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(Surface.copy(alpha = 0.70f))
            .padding(horizontal = 4.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, "Geri",
                tint = Primary, modifier = Modifier.size(24.dp)
            )
        }
        Text(
            "GYMBUDDY",
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
// Gün Kartı (tarih başlığı — tıklanınca açılır)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DayCard(
    date: String,
    mealCount: Int,
    totalCalories: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val displayDate = formatDateTurkish(date)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainerLow)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tarih ikonu
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth, null,
                    tint = Primary, modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = OnSurface
                )
                Text(
                    text = "$mealCount öğün",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = ManropeFamily
                    ),
                    color = OnSurfaceVariant
                )
            }

            // Toplam kalori chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Secondary.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$totalCalories kcal",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Secondary
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Öğün Kartı (tıklanınca detaylar açılır)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun MealCard(
    meal: MealEntry,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val mealTypeLabel = try {
        MealType.valueOf(meal.mealType).label
    } catch (_: Exception) { meal.mealType }

    val mealTypeColor = when (meal.mealType) {
        "BREAKFAST" -> Color(0xFFFFA726)
        "LUNCH"     -> Primary
        "DINNER"    -> Secondary
        "SNACK"     -> Color(0xFF7E57C2)
        else        -> OnSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        // ── Üst satır: fotoğraf + bilgi ─────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Yemek fotoğrafı
            if (meal.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(meal.photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = meal.mealName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Restaurant, null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Öğün tipi badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(mealTypeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = mealTypeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = mealTypeColor
                    )
                }
                Spacer(Modifier.height(4.dp))

                // Yemek isimleri
                Text(
                    text = meal.foodNames.joinToString(", ") {
                        it.substringBefore(" (")
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Saat
                val timeStr = formatTime(meal.timestamp)
                if (timeStr.isNotBlank()) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = ManropeFamily
                        ),
                        color = OnSurfaceVariant
                    )
                }
            }

            // Kalori
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${meal.calories}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black
                    ),
                    color = Secondary
                )
                Text(
                    text = "kcal",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ManropeFamily,
                        fontSize = 9.sp
                    ),
                    color = OnSurfaceVariant
                )
            }
        }

        // ── Açılır detay: besin değerleri ────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fotoğraf büyük gösterim
                if (meal.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(meal.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = meal.mealName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // Yemekler listesi
                Text(
                    text = "İÇERİK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontSize = 9.sp
                    ),
                    color = Primary
                )
                meal.foodNames.forEach { food ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = food,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = ManropeFamily
                            ),
                            color = OnSurface
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Makro bar
                Text(
                    text = "BESİN DEĞERLERİ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontSize = 9.sp
                    ),
                    color = Primary
                )

                // Ana makrolar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MacroChip("Protein", "%.1fg".format(meal.protein), Primary, Modifier.weight(1f))
                    MacroChip("Karb", "%.1fg".format(meal.carbs), Secondary, Modifier.weight(1f))
                    MacroChip("Yağ", "%.1fg".format(meal.fat), Color(0xFFFFA726), Modifier.weight(1f))
                }

                // Detay makrolar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MacroChip("Lif", "%.1fg".format(meal.fiber), OnSurfaceVariant, Modifier.weight(1f))
                    MacroChip("Şeker", "%.1fg".format(meal.sugar), OnSurfaceVariant, Modifier.weight(1f))
                    MacroChip("D.Yağ", "%.1fg".format(meal.saturatedFat), OnSurfaceVariant, Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MacroChip("Sodyum", "${meal.sodium}mg", OnSurfaceVariant, Modifier.weight(1f))
                    MacroChip("Kolest.", "${meal.cholesterol}mg", OnSurfaceVariant, Modifier.weight(1f))
                    MacroChip("Potasyum", "${meal.potassium}mg", OnSurfaceVariant, Modifier.weight(1f))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Makro Chip
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun MacroChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ManropeFamily,
                fontSize = 8.sp
            ),
            color = color.copy(alpha = 0.7f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Boş Durum
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyMealsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLow)
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.NoFood, null,
            tint = OnSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Henüz öğün kaydın yok",
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold
            ),
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tara sayfasından yemeklerini tarayarak kayıt oluşturabilirsin",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = ManropeFamily
            ),
            color = OnSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Yardımcılar
// ═══════════════════════════════════════════════════════════════════════

private fun formatDateTurkish(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale("tr"))
        val formatter = SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr"))
        val date = parser.parse(dateStr) ?: return dateStr
        formatter.format(date)
    } catch (_: Exception) { dateStr }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return try {
        val formatter = SimpleDateFormat("HH:mm", Locale("tr"))
        formatter.format(java.util.Date(timestamp))
    } catch (_: Exception) { "" }
}
