package com.example.gymbuddy.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymbuddy.data.model.WorkoutExercise
import com.example.gymbuddy.data.model.WorkoutSession
import com.example.gymbuddy.ui.theme.*
import com.example.gymbuddy.ui.viewmodel.WorkoutUiState
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

// ── Sabitler ────────────────────────────────────────────────────────

private val TR_MONTHS = mapOf(
    1 to "OCAK", 2 to "ŞUBAT", 3 to "MART", 4 to "NİSAN",
    5 to "MAYIS", 6 to "HAZİRAN", 7 to "TEMMUZ", 8 to "AĞUSTOS",
    9 to "EYLÜL", 10 to "EKİM", 11 to "KASIM", 12 to "ARALIK"
)
private val TR_DAYS_SHORT = mapOf(
    DayOfWeek.MONDAY to "Pzt", DayOfWeek.TUESDAY to "Sal",
    DayOfWeek.WEDNESDAY to "Çar", DayOfWeek.THURSDAY to "Per",
    DayOfWeek.FRIDAY to "Cum", DayOfWeek.SATURDAY to "Cmt",
    DayOfWeek.SUNDAY to "Paz"
)

private sealed class HistoryItem {
    data class MonthHeader(val key: String) : HistoryItem()
    data class SessionItem(val session: WorkoutSession) : HistoryItem()
}

// ═══════════════════════════════════════════════════════════════════════
// Main Screen
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    workoutState: WorkoutUiState,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val allSessions = remember(workoutState.sessions) {
        workoutState.sessions.sortedByDescending { it.dateTimestamp }
    }

    val totalVolume  = remember(allSessions) { allSessions.sumOf { it.totalVolumeKg.toDouble() }.toFloat() }
    val bestStreak   = remember(workoutState.workoutDates) { computeBestStreak(workoutState.workoutDates) }
    val avgPerWeek   = remember(allSessions) { computeAvgPerWeek(allSessions) }
    val topExercises = remember(allSessions) { computeTopExercises(allSessions) }
    val weeklyData   = remember(allSessions) { computeWeeklyVolume(allSessions) }

    var searchQuery       by remember { mutableStateOf("") }
    var expandedSessionId by remember { mutableStateOf<String?>(null) }

    // ── Tarih filtresi ──────────────────────────────────────────────
    var filterStartDate     by remember { mutableStateOf<LocalDate?>(null) }
    var filterEndDate       by remember { mutableStateOf<LocalDate?>(null) }
    var showStartPicker     by remember { mutableStateOf(false) }
    var showEndPicker       by remember { mutableStateOf(false) }

    val startPickerState = rememberDatePickerState(
        initialSelectedDateMillis = filterStartDate?.toEpochMillis()
    )
    val endPickerState = rememberDatePickerState(
        initialSelectedDateMillis = filterEndDate?.toEpochMillis()
    )

    val dateFilterActive = filterStartDate != null || filterEndDate != null

    // ── Birleşik filtre (arama + tarih) ────────────────────────────
    val filteredSessions = remember(allSessions, searchQuery, filterStartDate, filterEndDate) {
        allSessions.filter { s ->
            val matchesQuery = searchQuery.isBlank() ||
                s.exercises.any { it.name.contains(searchQuery, ignoreCase = true) }
            val sessionDate = try { LocalDate.parse(s.date) } catch (e: Exception) { null }
            val matchesStart = filterStartDate == null ||
                (sessionDate != null && !sessionDate.isBefore(filterStartDate))
            val matchesEnd   = filterEndDate == null ||
                (sessionDate != null && !sessionDate.isAfter(filterEndDate))
            matchesQuery && matchesStart && matchesEnd
        }
    }

    // ── Tarih seçici dialogları ─────────────────────────────────────
    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startPickerState.selectedDateMillis?.let {
                        filterStartDate = it.toLocalDate()
                        // Başlangıç, bitiş'ten sonraya çekilirse bitişi sıfırla
                        if (filterEndDate != null && filterStartDate!! > filterEndDate!!) {
                            filterEndDate = null
                        }
                    }
                    showStartPicker = false
                }) {
                    Text("Tamam", color = Primary,
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = LexendFamily, fontWeight = FontWeight.Black))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text("İptal", color = OnSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = LexendFamily))
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = SurfaceContainer
            )
        ) {
            DatePicker(
                state = startPickerState,
                colors = DatePickerDefaults.colors(
                    containerColor          = SurfaceContainer,
                    selectedDayContainerColor = Primary,
                    todayDateBorderColor    = Primary
                )
            )
        }
    }

    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endPickerState.selectedDateMillis?.let {
                        filterEndDate = it.toLocalDate()
                        if (filterStartDate != null && filterEndDate!! < filterStartDate!!) {
                            filterStartDate = null
                        }
                    }
                    showEndPicker = false
                }) {
                    Text("Tamam", color = Primary,
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = LexendFamily, fontWeight = FontWeight.Black))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text("İptal", color = OnSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = LexendFamily))
                }
            },
            colors = DatePickerDefaults.colors(containerColor = SurfaceContainer)
        ) {
            DatePicker(
                state = endPickerState,
                colors = DatePickerDefaults.colors(
                    containerColor          = SurfaceContainer,
                    selectedDayContainerColor = Primary,
                    todayDateBorderColor    = Primary
                )
            )
        }
    }

    val flatItems = remember(filteredSessions) {
        buildList {
            filteredSessions
                .groupBy { it.date.take(7) }
                .toSortedMap(reverseOrder())
                .forEach { (month, sessions) ->
                    add(HistoryItem.MonthHeader(month))
                    sessions.forEach { add(HistoryItem.SessionItem(it)) }
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp,
                bottom = 40.dp
            )
        ) {
            // ── Sayfa başlığı ──────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(
                        text = "ANTRENMAN",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp,
                            lineHeight = 34.sp
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = "GEÇMİŞİ",
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

            if (allSessions.isEmpty()) {
                item { EmptyHistoryContent() }
            } else {
                // ── Özet istatistikler ─────────────────────────────
                item {
                    StatsGrid(
                        totalWorkouts = allSessions.size,
                        totalVolume   = totalVolume,
                        bestStreak    = bestStreak,
                        avgPerWeek    = avgPerWeek,
                        modifier      = Modifier.padding(horizontal = 20.dp)
                    )
                }

                item { Spacer(Modifier.height(20.dp)) }

                // ── Haftalık hacim grafiği ─────────────────────────
                item {
                    VolumeChartCard(
                        weeklyData = weeklyData,
                        modifier   = Modifier.padding(horizontal = 20.dp)
                    )
                }

                item { Spacer(Modifier.height(20.dp)) }

                // ── En çok çalışılan egzersizler ───────────────────
                if (topExercises.isNotEmpty()) {
                    item {
                        TopExercisesCard(
                            exercises = topExercises,
                            modifier  = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }

                // ── Arama ──────────────────────────────────────────
                item {
                    HistorySearchBar(
                        query         = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier      = Modifier.padding(horizontal = 20.dp)
                    )
                }

                // ── Tarih filtresi ─────────────────────────────────
                item {
                    DateFilterRow(
                        startDate    = filterStartDate,
                        endDate      = filterEndDate,
                        onStartClick = { showStartPicker = true },
                        onEndClick   = { showEndPicker = true },
                        onClear      = { filterStartDate = null; filterEndDate = null },
                        modifier     = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(top = 8.dp)
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }

                // ── Session listesi ────────────────────────────────
                if (flatItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val noResultMsg = when {
                                searchQuery.isNotBlank() && dateFilterActive ->
                                    "\"$searchQuery\" araması ve seçilen tarih aralığı için sonuç bulunamadı"
                                searchQuery.isNotBlank() ->
                                    "\"$searchQuery\" için sonuç bulunamadı"
                                dateFilterActive ->
                                    "Seçilen tarih aralığında antrenman bulunamadı"
                                else -> "Sonuç bulunamadı"
                            }
                            Text(
                                noResultMsg,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = ManropeFamily
                                ),
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(
                        flatItems,
                        key = { item ->
                            when (item) {
                                is HistoryItem.MonthHeader -> "h_${item.key}"
                                is HistoryItem.SessionItem -> item.session.id
                            }
                        }
                    ) { item ->
                        when (item) {
                            is HistoryItem.MonthHeader -> MonthHeaderRow(
                                yearMonth = item.key,
                                modifier  = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                            is HistoryItem.SessionItem -> SessionCard(
                                session    = item.session,
                                isExpanded = expandedSessionId == item.session.id,
                                onToggle   = {
                                    expandedSessionId =
                                        if (expandedSessionId == item.session.id) null
                                        else item.session.id
                                },
                                modifier   = Modifier
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Sabit Top Bar ───────────────────────────────────────────
        HistoryTopBar(onBack)
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Top Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun HistoryTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(Surface.copy(alpha = 0.70f))
            .padding(horizontal = 4.dp),
    ) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Geri",
                tint               = Primary,
                modifier           = Modifier.size(24.dp)
            )
        }
        Text(
            text     = "GYMBUDDY",
            style    = MaterialTheme.typography.titleLarge.copy(
                fontFamily  = LexendFamily,
                fontWeight  = FontWeight.Black,
                letterSpacing = (-1).sp,
                fontStyle   = FontStyle.Italic
            ),
            color    = Primary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// İstatistik Grid (2×2)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun StatsGrid(
    totalWorkouts: Int,
    totalVolume: Float,
    bestStreak: Int,
    avgPerWeek: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                value    = "$totalWorkouts",
                label    = "TOPLAM\nANTRENMAN",
                valueColor = Primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value    = formatVolume(totalVolume),
                label    = "TOPLAM\nHACİM",
                valueColor = Secondary,
                hasAccent = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                value    = "$bestStreak gün",
                label    = "EN UZUN\nSERİ",
                valueColor = Primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value    = "${"%.1f".format(avgPerWeek)}/hafta",
                label    = "HAFTALIK\nORTALAMA",
                valueColor = Primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    valueColor: androidx.compose.ui.graphics.Color,
    hasAccent: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .then(
                if (hasAccent) Modifier.drawBehind {
                    drawRoundRect(
                        color        = Secondary.copy(alpha = 0.15f),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style        = Stroke(width = 1.dp.toPx())
                    )
                } else Modifier
            )
            .padding(vertical = 16.dp, horizontal = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text  = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                fontSize   = 22.sp
            ),
            color = valueColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily    = ManropeFamily,
                fontWeight    = FontWeight.Bold,
                fontSize      = 9.sp,
                letterSpacing = 0.8.sp,
                lineHeight    = 13.sp
            ),
            color = OnSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Haftalık Hacim Grafiği
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun VolumeChartCard(
    weeklyData: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLow)
            .padding(20.dp)
    ) {
        Text(
            text  = "HAFTALIK HACİM",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily    = LexendFamily,
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp,
                fontSize      = 10.sp
            ),
            color = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        val maxVol = weeklyData.maxOfOrNull { it.second }?.takeIf { it > 0f } ?: 1f
        val currentWeekVol = weeklyData.lastOrNull()?.second ?: 0f

        // Kısa özet
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text  = formatVolume(currentWeekVol),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black
                ),
                color = Primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text  = "bu hafta",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bar chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val barCount   = weeklyData.size
            val totalGap   = 6.dp.toPx() * (barCount - 1)
            val barWidth   = (size.width - totalGap) / barCount
            val maxBarH    = size.height - 20.dp.toPx()

            weeklyData.forEachIndexed { i, (_, vol) ->
                val barH    = if (maxVol > 0f) (vol / maxVol) * maxBarH else 0f
                val left    = i * (barWidth + 6.dp.toPx())
                val top     = size.height - barH
                val isCurrent = i == weeklyData.lastIndex
                val color   = if (isCurrent) Primary else SurfaceContainerHigh
                val radius  = 6.dp.toPx()

                if (barH > 0f) {
                    drawRoundRect(
                        color        = color,
                        topLeft      = Offset(left, top),
                        size         = Size(barWidth, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
                    )
                } else {
                    // Boş bar — ince çizgi
                    drawRoundRect(
                        color        = SurfaceContainerHighest,
                        topLeft      = Offset(left, size.height - 4.dp.toPx()),
                        size         = Size(barWidth, 4.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // X ekseni etiketleri
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weeklyData.forEach { (label, _) ->
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ManropeFamily,
                        fontSize   = 9.sp
                    ),
                    color    = OnSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// En Çok Çalışılan Egzersizler
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun TopExercisesCard(
    exercises: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLow)
            .padding(20.dp)
    ) {
        Text(
            text  = "EN ÇOK ÇALIŞILAN EGZERSİZLER",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily    = LexendFamily,
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp,
                fontSize      = 10.sp
            ),
            color = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        exercises.forEachIndexed { index, (name, count) ->
            val barFraction = count.toFloat() / (exercises.firstOrNull()?.second?.toFloat() ?: 1f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (index < exercises.lastIndex) 10.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sıra numarası
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (index == 0) Primary.copy(alpha = 0.15f)
                            else SurfaceContainerHighest
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black,
                            fontSize   = 10.sp
                        ),
                        color = if (index == 0) Primary else OnSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                // Bar + isim
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color    = OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text  = "$count seans",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = ManropeFamily,
                                fontSize   = 10.sp
                            ),
                            color = OnSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceContainerHighest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (index == 0) Primary else Primary.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Arama Çubuğu
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun HistorySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint     = OnSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value         = query,
            onValueChange = onQueryChange,
            singleLine    = true,
            textStyle     = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium,
                color      = OnSurface
            ),
            cursorBrush   = SolidColor(Primary),
            modifier      = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        "Egzersiz adına göre ara…",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                inner()
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Tarih Filtresi Satırı
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DateFilterRow(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale("tr"))
    val isActive = startDate != null || endDate != null

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Başlangıç tarihi butonu
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (startDate != null) Primary.copy(alpha = 0.12f)
                    else SurfaceContainerLow
                )
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onStartClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint     = if (startDate != null) Primary else OnSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text  = startDate?.format(dateFmt) ?: "Başlangıç",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 11.sp
                ),
                color    = if (startDate != null) Primary else OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Ayırıcı
        Text(
            "→",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant.copy(alpha = 0.5f)
        )

        // Bitiş tarihi butonu
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (endDate != null) Primary.copy(alpha = 0.12f)
                    else SurfaceContainerLow
                )
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onEndClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint     = if (endDate != null) Primary else OnSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text  = endDate?.format(dateFmt) ?: "Bitiş",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 11.sp
                ),
                color    = if (endDate != null) Primary else OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Temizle butonu (sadece filtre aktifken görünür)
        AnimatedVisibility(visible = isActive) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OnSurfaceVariant.copy(alpha = 0.10f))
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onClear() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Clear,
                    contentDescription = "Filtreyi temizle",
                    tint     = OnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Ay Başlığı
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun MonthHeaderRow(yearMonth: String, modifier: Modifier = Modifier) {
    val parts  = yearMonth.split("-")
    val year   = parts.getOrNull(0) ?: ""
    val month  = parts.getOrNull(1)?.toIntOrNull()
    val label  = "${TR_MONTHS[month] ?: yearMonth} $year"

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily    = LexendFamily,
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp,
                fontSize      = 11.sp
            ),
            color = Primary
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Primary.copy(alpha = 0.15f))
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Session Kartı (Genişleyebilir)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SessionCard(
    session: WorkoutSession,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (dayNum, dayName, _) = parseSessionDate(session.date)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainer)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggle() }
    ) {
        // ── Özet satırı ───────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tarih badge
            Column(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary.copy(alpha = 0.12f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text  = dayNum,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        fontSize   = 18.sp
                    ),
                    color = Primary
                )
                Text(
                    text  = dayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 9.sp
                    ),
                    color = Primary.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Bilgiler
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = session.displayName(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color    = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Meta: egzersiz sayısı · set · hacim
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    MetaChip("${session.exercises.size} egzersiz")
                    MetaDot()
                    MetaChip("${session.totalSets()} set")
                    MetaDot()
                    MetaChip(formatVolume(session.totalVolumeKg), isAccent = true)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Egzersiz çipleri (ilk 2 + fazlası)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    session.exercises.take(2).forEach { ex ->
                        ExerciseNameChip(ex.name)
                    }
                    if (session.exercises.size > 2) {
                        ExerciseNameChip("+${session.exercises.size - 2}")
                    }
                }
            }

            // Expand/collapse ikonu
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp
                              else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Kapat" else "Aç",
                tint     = OnSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        // ── Detay (expandable) ───────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter   = expandVertically(tween(220)),
            exit    = shrinkVertically(tween(180))
        ) {
            SessionDetailContent(session)
        }
    }
}

@Composable
private fun MetaChip(text: String, isAccent: Boolean = false) {
    Text(
        text  = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 10.sp
        ),
        color = if (isAccent) Secondary else OnSurfaceVariant
    )
}

@Composable
private fun MetaDot() {
    Text("·", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.4f))
}

@Composable
private fun ExerciseNameChip(name: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceContainerHigh)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text  = name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 9.sp
            ),
            color    = OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Session Detay (expanded)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SessionDetailContent(session: WorkoutSession) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLow.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Toplam hacim özeti
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Primary.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryCell("TOPLAM HACİM", formatVolume(session.totalVolumeKg))
            SummaryCell("EGZERSİZ", "${session.exercises.size}", center = true)
            SummaryCell("SET", "${session.totalSets()}", align = TextAlign.End)
        }

        // Egzersiz detayları
        session.exercises.forEach { exercise ->
            ExerciseDetailSection(exercise)
        }
    }
}

@Composable
private fun SummaryCell(
    label: String,
    value: String,
    center: Boolean = false,
    align: TextAlign = TextAlign.Start
) {
    Column(horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start) {
        Text(
            text  = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                fontSize   = 15.sp
            ),
            color     = Primary,
            textAlign = align
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily    = ManropeFamily,
                fontSize      = 9.sp,
                letterSpacing = 0.5.sp
            ),
            color     = OnSurfaceVariant,
            textAlign = align
        )
    }
}

@Composable
private fun ExerciseDetailSection(exercise: WorkoutExercise) {
    Column {
        // Egzersiz adı
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Primary)
            )
            Text(
                text  = exercise.name.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily    = LexendFamily,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontSize      = 10.sp
                ),
                color = OnSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Set tablosu başlığı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(SurfaceContainerHighest)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            TableHeader("SET", Modifier.width(36.dp))
            TableHeader("AĞIRLIK", Modifier.weight(1f))
            TableHeader("TEKRAR", Modifier.weight(1f), TextAlign.End)
        }

        exercise.sets.forEachIndexed { idx, set ->
            val isLast = idx == exercise.sets.lastIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (idx % 2 == 0) SurfaceContainerLow
                        else SurfaceContainer
                    )
                    .then(
                        if (isLast) Modifier.clip(
                            RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                        ) else Modifier
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "${set.setNumber}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black
                    ),
                    color    = Primary,
                    modifier = Modifier.width(36.dp)
                )
                Text(
                    text  = if (set.weightKg.isNotBlank()) "${set.weightKg} kg" else "—",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color    = OnSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text  = if (set.reps.isNotBlank()) "${set.reps} tekrar" else "—",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color     = OnSurface,
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun TableHeader(
    text: String,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text  = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily    = LexendFamily,
            fontWeight    = FontWeight.Black,
            fontSize      = 9.sp,
            letterSpacing = 1.sp
        ),
        color     = OnSurfaceVariant,
        modifier  = modifier,
        textAlign = align
    )
}

// ═══════════════════════════════════════════════════════════════════════
// Boş durum
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyHistoryContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint     = OnSurfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.size(72.dp)
            )
            Text(
                text  = "Henüz antrenman yok",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black
                ),
                color = OnSurface
            )
            Text(
                text  = "Ana sayfadan ilk antrenmanını kaydet ve burada tüm geçmişini gör.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = ManropeFamily,
                    lineHeight = 18.sp
                ),
                color     = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Yardımcı Fonksiyonlar
// ═══════════════════════════════════════════════════════════════════════

private fun computeBestStreak(workoutDates: Set<String>): Int {
    if (workoutDates.isEmpty()) return 0
    val sorted = workoutDates.mapNotNull {
        try { LocalDate.parse(it) } catch (e: Exception) { null }
    }.sorted()
    if (sorted.isEmpty()) return 0
    var maxStreak = 1
    var cur = 1
    for (i in 1 until sorted.size) {
        cur = if (sorted[i] == sorted[i - 1].plusDays(1)) cur + 1 else 1
        if (cur > maxStreak) maxStreak = cur
    }
    return maxStreak
}

private fun computeAvgPerWeek(sessions: List<WorkoutSession>): Float {
    if (sessions.isEmpty()) return 0f
    val first = sessions.minByOrNull { it.dateTimestamp }
        ?.let { try { LocalDate.parse(it.date) } catch (e: Exception) { null } }
        ?: return 0f
    val totalDays  = ChronoUnit.DAYS.between(first, LocalDate.now()).toInt()
    val totalWeeks = maxOf(1f, totalDays / 7f)
    return sessions.size / totalWeeks
}

private fun computeTopExercises(sessions: List<WorkoutSession>): List<Pair<String, Int>> {
    return sessions
        .flatMap { it.exercises }
        .groupBy { it.name.trim().lowercase() }
        .map { (_, list) ->
            val display = list.first().name.trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            display to list.size
        }
        .sortedByDescending { it.second }
        .take(6)
}

private fun computeWeeklyVolume(sessions: List<WorkoutSession>): List<Pair<String, Float>> {
    val today = LocalDate.now()
    return (7 downTo 0).map { weeksAgo ->
        val weekStart = today
            .minusWeeks(weeksAgo.toLong())
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val volume = sessions.filter { s ->
            try {
                val d = LocalDate.parse(s.date)
                !d.isBefore(weekStart) && !d.isAfter(weekEnd)
            } catch (e: Exception) { false }
        }.sumOf { it.totalVolumeKg.toDouble() }.toFloat()
        val label = "${weekStart.dayOfMonth}/${weekStart.monthValue}"
        label to volume
    }
}

private fun formatVolume(kg: Float): String {
    return when {
        kg >= 1000f -> "${"%.1f".format(kg / 1000f)}k kg"
        kg > 0f     -> "${kg.toInt()} kg"
        else        -> "0 kg"
    }
}

/** "2026-05-26" → (dayNum="26", dayName="Pzt") */
private fun parseSessionDate(dateStr: String): Triple<String, String, String> {
    return try {
        val date = LocalDate.parse(dateStr)
        val dayNum  = date.dayOfMonth.toString()
        val dayName = TR_DAYS_SHORT[date.dayOfWeek] ?: ""
        Triple(dayNum, dayName, "$dayNum $dayName")
    } catch (e: Exception) {
        Triple("?", "?", dateStr)
    }
}

// ── DatePicker için uzantı fonksiyonlar ─────────────────────────────────

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
