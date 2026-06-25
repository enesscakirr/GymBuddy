package com.example.gymbuddy.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gymbuddy.data.model.WorkoutSession
import com.example.gymbuddy.ui.theme.*
import com.example.gymbuddy.ui.viewmodel.DraftExercise
import com.example.gymbuddy.ui.viewmodel.DraftSet
import com.example.gymbuddy.ui.viewmodel.WorkoutUiState
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import java.util.UUID

// ── Takip isteği bildirim modeli ─────────────────────────────────────
data class FollowRequestNotif(
    val fromUid: String,
    val fromName: String,
    val fromPhotoUrl: String = "",
    val timestamp: Long = 0L
)

// ═══════════════════════════════════════════════════════════════════════
// HomeScreen
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String = "",
    workoutState: WorkoutUiState = WorkoutUiState(),
    followRequests: List<FollowRequestNotif> = emptyList(),
    onAcceptRequest: (String) -> Unit = {},
    onRejectRequest: (String) -> Unit = {},
    onSaveWorkout: (LocalDate, List<DraftExercise>) -> Unit = { _, _ -> },
    onClearSaveSuccess: () -> Unit = {}
) {
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    // Draft egzersizler
    var draftExercises by remember {
        mutableStateOf(
            listOf(DraftExercise(id = UUID.randomUUID().toString(), name = "", sets = listOf(DraftSet(1))))
        )
    }

    // Seçili tarihe ait antrenmanlar
    val sessionsForDate = workoutState.sessions.filter {
        it.date == selectedDate.format(fmt)
    }

    val isNewUser = workoutState.isLoaded && workoutState.totalWorkouts == 0
    val scrollState = rememberScrollState()

    // Detay sheet için seçili session
    var detailSession by remember { mutableStateOf<WorkoutSession?>(null) }

    // Bildirim sheet
    var showNotifications by remember { mutableStateOf(false) }

    // Bildirim badge hesapla
    val todayStr = today.format(fmt)
    val workedOutToday   = workoutState.sessions.any { it.date == todayStr }
    val hasActiveNotifs  = !workedOutToday || workoutState.currentStreak > 0 || followRequests.isNotEmpty()

    // Kayıt başarılı → 2 sn sonra buton sıfırla + formu temizle
    LaunchedEffect(workoutState.saveSuccess) {
        if (workoutState.saveSuccess) {
            delay(2000)
            onClearSaveSuccess()
            draftExercises = listOf(
                DraftExercise(id = UUID.randomUUID().toString(), name = "", sets = listOf(DraftSet(1)))
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            HomeTopBar(
                hasNotifications    = hasActiveNotifs,
                onNotificationClick = { showNotifications = true }
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 100.dp)
            ) {
            // Welcome Banner (sadece yeni kullanıcı — veri yüklendikten sonra)
            if (isNewUser) {
                WelcomeBanner(userName = userName)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Takvim
            CalendarStrip(
                selectedDate = selectedDate,
                workoutDates = workoutState.workoutDates,
                onDateSelected = { selectedDate = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Antrenman kayıt kartı
            LogSessionCard(
                selectedDate = selectedDate,
                draftExercises = draftExercises,
                isSaving = workoutState.isSaving,
                saveSuccess = workoutState.saveSuccess,
                onExerciseNameChange = { idx, name ->
                    draftExercises = draftExercises.toMutableList().also {
                        it[idx] = it[idx].copy(name = name)
                    }
                },
                onSetValueChange = { exIdx, setIdx, updatedSet ->
                    draftExercises = draftExercises.toMutableList().also { exList ->
                        val ex = exList[exIdx]
                        val newSets = ex.sets.toMutableList().also { it[setIdx] = updatedSet }
                        exList[exIdx] = ex.copy(sets = newSets)
                    }
                },
                onAddSet = { exIdx ->
                    draftExercises = draftExercises.toMutableList().also { exList ->
                        val ex = exList[exIdx]
                        exList[exIdx] = ex.copy(sets = ex.sets + DraftSet(ex.sets.size + 1))
                    }
                },
                onRemoveExercise = { exIdx ->
                    if (draftExercises.size > 1) {
                        draftExercises = draftExercises.toMutableList().also { it.removeAt(exIdx) }
                    }
                },
                onAddExercise = {
                    draftExercises = draftExercises + DraftExercise(
                        id = UUID.randomUUID().toString(),
                        name = "",
                        sets = listOf(DraftSet(1))
                    )
                },
                onFinishWorkout = {
                    onSaveWorkout(selectedDate, draftExercises)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Seçili güne ait antrenmanlar
            if (sessionsForDate.isNotEmpty()) {
                DaySessionsCard(
                    sessions = sessionsForDate,
                    selectedDate = selectedDate,
                    onSessionClick = { detailSession = it }
                )
            } else {
                EmptyDayCard(selectedDate = selectedDate)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Volume Trend
            VolumeTrendCard(sessions = workoutState.sessions)

            Spacer(modifier = Modifier.height(24.dp))

            // Haftalık İlerleme
            WeeklyProgressCard(sessions = workoutState.sessions)

            Spacer(modifier = Modifier.height(24.dp))

            // Streak + PR
            QuickStatsRow(
                streak = workoutState.currentStreak,
                prKg = workoutState.prTodayKg
            )

            Spacer(modifier = Modifier.height(16.dp))
            } // inner Column
        }
    }

    // Detay bottom sheet
    detailSession?.let { session ->
        WorkoutDetailSheet(
            session = session,
            onDismiss = { detailSession = null }
        )
    }

    // Bildirim bottom sheet
    if (showNotifications) {
        NotificationsSheet(
            workoutState    = workoutState,
            followRequests  = followRequests,
            onAcceptRequest = onAcceptRequest,
            onRejectRequest = onRejectRequest,
            onDismiss       = { showNotifications = false }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Top Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun HomeTopBar(
    hasNotifications: Boolean,
    onNotificationClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Surface.copy(alpha = 0.70f))
            .padding(horizontal = 4.dp)
    ) {
        // Logo — tam ortalı
        Text(
            text = "GYMBUDDY",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily    = LexendFamily,
                fontWeight    = FontWeight.Black,
                letterSpacing = (-1).sp,
                fontStyle     = FontStyle.Italic
            ),
            color    = Primary,
            modifier = Modifier.align(Alignment.Center)
        )

        // Bildirim butonu — sağ
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = onNotificationClick) {
                Icon(
                    imageVector        = if (hasNotifications) Icons.Outlined.Notifications
                                        else Icons.Outlined.NotificationsNone,
                    contentDescription = "Bildirimler",
                    tint               = Primary
                )
            }
            if (hasNotifications) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-10).dp, y = 10.dp)
                        .clip(CircleShape)
                        .background(Secondary)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Calendar Strip — hafta navigasyonu + tam tarih seçici
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CalendarStrip(
    selectedDate: LocalDate,
    workoutDates: Set<String>,
    onDateSelected: (LocalDate) -> Unit
) {
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    // weekOffset: kaç hafta kaydırıldığı (0 = seçili tarihin haftası)
    var weekOffset by remember { mutableStateOf(0) }
    var showFullPicker by remember { mutableStateOf(false) }

    val baseMonday = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
    val weekStart = baseMonday.plusWeeks(weekOffset.toLong())

    Column(modifier = Modifier.fillMaxWidth()) {
        // Başlık satırı
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ay adı + yıl
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = selectedDate.month
                        .getDisplayName(JavaTextStyle.FULL, Locale.getDefault())
                        .uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily    = LexendFamily,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${selectedDate.year}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = OnSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Önceki hafta
                IconButton(
                    onClick = { weekOffset-- },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Önceki hafta",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Tam takvim butonu
                IconButton(
                    onClick = { showFullPicker = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = "Takvim",
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Sonraki hafta
                IconButton(
                    onClick = { weekOffset++ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Sonraki hafta",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Haftalık günler
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 0..6) {
                val date = weekStart.plusDays(i.toLong())
                val isSelected = date == selectedDate
                val hasWorkout = workoutDates.contains(date.format(fmt))
                val isToday = date == LocalDate.now()
                val dayName = date.dayOfWeek
                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                    .uppercase()

                Column(
                    modifier = Modifier
                        .width(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                isSelected -> Primary
                                isToday -> Primary.copy(alpha = 0.15f)
                                else -> SurfaceContainerLow
                            }
                        )
                        .clickable {
                            onDateSelected(date)
                            weekOffset = 0 // seçilen tarihin haftasına dön
                        }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (isSelected) OnPrimary else OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${date.dayOfMonth}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black
                        ),
                        color = if (isSelected) OnPrimary else OnSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected && hasWorkout -> OnPrimary
                                    hasWorkout -> Secondary
                                    else -> Color.Transparent
                                }
                            )
                    )
                }
            }
        }
    }

    // Tam takvim dialog
    if (showFullPicker) {
        FullCalendarDialog(
            selectedDate = selectedDate,
            workoutDates = workoutDates,
            onDateSelected = { date ->
                onDateSelected(date)
                weekOffset = 0
                showFullPicker = false
            },
            onDismiss = { showFullPicker = false }
        )
    }
}

// ── Tam Takvim Dialog (ay bazlı) ───────────────────────────────────────

@Composable
private fun FullCalendarDialog(
    selectedDate: LocalDate,
    workoutDates: Set<String>,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    var displayMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val dayNames = listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { displayMonth = displayMonth.minusMonths(1) }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = Primary)
                    }

                    Text(
                        text = "${displayMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()).uppercase()} ${displayMonth.year}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black
                        ),
                        color = Primary
                    )

                    IconButton(onClick = { displayMonth = displayMonth.plusMonths(1) }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Primary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gün başlıkları
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayNames.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = OnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Takvim grid
                val firstDay = displayMonth.atDay(1)
                val startOffset = firstDay.dayOfWeek.value - 1 // Pazartesi = 0
                val daysInMonth = displayMonth.lengthOfMonth()
                val totalCells = startOffset + daysInMonth
                val rows = (totalCells + 6) / 7

                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - startOffset + 1

                            if (dayNum < 1 || dayNum > daysInMonth) {
                                Box(modifier = Modifier.weight(1f).height(40.dp))
                            } else {
                                val date = displayMonth.atDay(dayNum)
                                val isSelected = date == selectedDate
                                val hasWorkout = workoutDates.contains(date.format(fmt))
                                val isToday = date == LocalDate.now()

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected -> Primary
                                                isToday -> Primary.copy(alpha = 0.15f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onDateSelected(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$dayNum",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = LexendFamily,
                                                fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Medium,
                                                fontSize = 13.sp
                                            ),
                                            color = when {
                                                isSelected -> OnPrimary
                                                isToday -> Primary
                                                else -> OnSurface
                                            }
                                        )
                                        if (hasWorkout) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) OnPrimary else Secondary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Kapat butonu
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "KAPAT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black
                        ),
                        color = Primary
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Log Session Card
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun LogSessionCard(
    selectedDate: LocalDate,
    draftExercises: List<DraftExercise>,
    isSaving: Boolean,
    saveSuccess: Boolean,
    onExerciseNameChange: (Int, String) -> Unit,
    onSetValueChange: (Int, Int, DraftSet) -> Unit,
    onAddSet: (Int) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onAddExercise: () -> Unit,
    onFinishWorkout: () -> Unit
) {
    val dateLabel = when (selectedDate) {
        LocalDate.now() -> "Bugün"
        LocalDate.now().minusDays(1) -> "Dün"
        else -> selectedDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
    ) {
        Icon(
            imageVector = Icons.Outlined.FitnessCenter,
            contentDescription = null,
            tint = OnSurface.copy(alpha = 0.05f),
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = (-10).dp)
        )

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "ANTRENMAN KAYDET",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = OnSurface
            )
            Text(
                text = dateLabel.uppercase(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = Primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            draftExercises.forEachIndexed { exIdx, exercise ->
                ExerciseBlock(
                    exercise = exercise,
                    showRemove = draftExercises.size > 1,
                    onNameChange = { name -> onExerciseNameChange(exIdx, name) },
                    onSetValueChange = { setIdx, updatedSet ->
                        onSetValueChange(exIdx, setIdx, updatedSet)
                    },
                    onAddSet = { onAddSet(exIdx) },
                    onRemove = { onRemoveExercise(exIdx) }
                )
                if (exIdx < draftExercises.lastIndex) Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // + EGZERSİZ EKLE butonu — card içinde, kaydet butonunun üstünde
            OutlinedButton(
                onClick = onAddExercise,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = Primary.copy(alpha = 0.40f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EGZERSİZ EKLE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // İnce ayırıcı
            HorizontalDivider(
                color = OutlineVariant.copy(alpha = 0.25f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onFinishWorkout,
                enabled = !isSaving && !saveSuccess,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (saveSuccess) Secondary else Primary,
                    disabledContainerColor = if (saveSuccess) Secondary else Primary.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = OnPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = if (saveSuccess) "✓  KAYDEDİLDİ" else "ANTRENMANI BİTİR",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 14.sp
                        ),
                        color = OnPrimary
                    )
                }
            }
        }
    }
}

// ── Tek egzersiz bloğu ─────────────────────────────────────────────────

@Composable
private fun ExerciseBlock(
    exercise: DraftExercise,
    showRemove: Boolean,
    onNameChange: (String) -> Unit,
    onSetValueChange: (Int, DraftSet) -> Unit,
    onAddSet: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicTextField(
                value = exercise.name,
                onValueChange = onNameChange,
                textStyle = TextStyle(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Primary
                ),
                singleLine = true,
                cursorBrush = SolidColor(Primary),
                decorationBox = { inner ->
                    if (exercise.name.isEmpty()) {
                        Text(
                            text = "Egzersiz adı gir…",
                            style = TextStyle(
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = OnSurfaceVariant.copy(alpha = 0.45f)
                            )
                        )
                    }
                    inner()
                },
                modifier = Modifier.weight(1f)
            )

            if (showRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Sil",
                        tint = OnSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("SET", "ÖNCEKİ", "KG", "TEKRAR").forEach { h ->
                Text(
                    text = h,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    ),
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        exercise.sets.forEachIndexed { setIdx, set ->
            SetRow(
                set = set,
                onWeightChange = { w -> onSetValueChange(setIdx, set.copy(weightKg = w)) },
                onRepsChange = { r -> onSetValueChange(setIdx, set.copy(reps = r)) }
            )
            if (setIdx < exercise.sets.lastIndex) Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, OutlineVariant.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .clickable { onAddSet() }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ SET EKLE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                ),
                color = OnSurfaceVariant
            )
        }
    }
}

// ── Set Satırı ────────────────────────────────────────────────────────

@Composable
private fun SetRow(
    set: DraftSet,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .background(SurfaceContainerHigh, RoundedCornerShape(6.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${set.setNumber}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = OnSurface
            )
        }
        Box(
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = OnSurfaceVariant)
        }
        SetInputField(value = set.weightKg, onValueChange = onWeightChange, placeholder = "0", modifier = Modifier.weight(1f))
        SetInputField(value = set.reps, onValueChange = onRepsChange, placeholder = "0", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SetInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .background(SurfaceContainerHigh, RoundedCornerShape(6.dp))
            .padding(vertical = 8.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = Primary,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = SolidColor(Primary),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = OnSurfaceVariant.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                innerTextField()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Seçili güne ait antrenman listesi
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DaySessionsCard(
    sessions: List<WorkoutSession>,
    selectedDate: LocalDate,
    onSessionClick: (WorkoutSession) -> Unit
) {
    val today = LocalDate.now()
    val daysAgo = today.toEpochDay() - selectedDate.toEpochDay()
    val dateLabel = when {
        daysAgo == 0L -> "Bugünün Antrenmanları"
        daysAgo == 1L -> "Dünün Antrenmanları"
        daysAgo < 0L -> "${selectedDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))} Antrenmanları"
        else -> "${selectedDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))} Antrenmanları"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .padding(20.dp)
    ) {
        Text(
            text = dateLabel.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            color = OnSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        sessions.forEach { session ->
            val exerciseCount = session.exercises.size
            val setCount = session.exercises.sumOf { it.sets.size }
            val exerciseNames = session.exercises.take(2)
                .joinToString(" + ") { it.name.ifBlank { "Egzersiz" } }
                .let { if (session.exercises.size > 2) "$it +${session.exercises.size - 2}" else it }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { onSessionClick(session) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Secondary.copy(alpha = 0.20f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = Secondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseNames.ifBlank { "Antrenman" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    Text(
                        text = "$exerciseCount egzersiz · $setCount set".uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                        ),
                        color = OnSurfaceVariant
                    )
                }

                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmptyDayCard(selectedDate: LocalDate) {
    val today = LocalDate.now()
    val daysAgo = today.toEpochDay() - selectedDate.toEpochDay()
    val label = when {
        daysAgo == 0L -> "Bugün henüz antrenman yok"
        daysAgo > 0L -> "Bu güne ait kayıt yok"
        else -> "Gelecek tarih seçildi"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GÜNÜN ANTRENMANLARı",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            color = OnSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = OnSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium
            ),
            color = OnSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Workout Detay Bottom Sheet
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutDetailSheet(
    session: WorkoutSession,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormatted = try {
        val d = LocalDate.parse(session.date, DateTimeFormatter.ISO_LOCAL_DATE)
        d.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale.getDefault()))
    } catch (e: Exception) {
        session.date
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceContainerLow,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OutlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "ANTRENMAN DETAYI",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Kapat", tint = OnSurfaceVariant)
                }
            }

            // Özet satırı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryChip(label = "Egzersiz", value = "${session.exercises.size}")
                SummaryChip(label = "Set", value = "${session.exercises.sumOf { it.sets.size }}")
                if (session.totalVolumeKg > 0f) {
                    SummaryChip(label = "Toplam Hacim", value = "${session.totalVolumeKg.toInt()} kg")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Egzersizler
            session.exercises.forEach { exercise ->
                ExerciseDetailBlock(exercise = exercise)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black
            ),
            color = Primary
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun ExerciseDetailBlock(exercise: com.example.gymbuddy.data.model.WorkoutExercise) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .padding(16.dp)
    ) {
        // Egzersiz adı
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = exercise.name.ifBlank { "Egzersiz" },
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = Primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tablo başlıkları
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("SET", "KG", "TEKRAR", "HACİM").forEach { h ->
                Text(
                    text = h,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = OnSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Set satırları
        exercise.sets.forEachIndexed { idx, set ->
            val kg = set.weightKg.toFloatOrNull() ?: 0f
            val reps = set.reps.toIntOrNull() ?: 0
            val volume = kg * reps

            val bgColor = if (idx % 2 == 0) SurfaceContainerHigh else Color.Transparent

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Set numarası
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.15f))
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${set.setNumber}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black
                        ),
                        color = Primary,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = if (set.weightKg.isBlank()) "-" else "${set.weightKg} kg",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface
                )
                Text(
                    text = if (set.reps.isBlank()) "-" else "${set.reps}×",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface
                )
                Text(
                    text = if (volume > 0f) "${volume.toInt()} kg" else "-",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = OnSurfaceVariant
                )
            }

            if (idx < exercise.sets.lastIndex) {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Volume Trend
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun VolumeTrendCard(sessions: List<WorkoutSession>) {
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    val last7 = (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        val volume = sessions.filter { it.date == date.format(fmt) }
            .sumOf { it.totalVolumeKg.toDouble() }.toFloat()
        date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
            .first().uppercaseChar().toString() to volume
    }

    val maxVolume = last7.maxOfOrNull { it.second } ?: 1f
    val safeMax = if (maxVolume == 0f) 1f else maxVolume
    val maxIdx = last7.indices.maxByOrNull { last7[it].second } ?: -1

    val thisWeekVol = last7.sumOf { it.second.toDouble() }
    val prevWeekSessions = sessions.filter {
        val d = LocalDate.parse(it.date, fmt)
        d.isBefore(today.minusDays(6)) && d.isAfter(today.minusDays(14))
    }
    val prevWeekVol = prevWeekSessions.sumOf { it.totalVolumeKg.toDouble() }
    val trendStr = when {
        prevWeekVol > 0 -> {
            val pct = ((thisWeekVol - prevWeekVol) / prevWeekVol * 100).toInt()
            if (pct >= 0) "+$pct%" else "$pct%"
        }
        thisWeekVol > 0 -> "+∞"
        else -> "—"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text(
                    text = "HACIM TRENDİ",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp
                    ),
                    color = OnSurface
                )
                Text(
                    text = "SON 7 GÜN",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    color = OnSurfaceVariant
                )
            }
            Text(
                text = trendStr,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = LexendFamily, fontWeight = FontWeight.Black),
                color = Primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Grafik alanı — her sütun: değer etiketi (üst) + çubuk (alt)
        Row(
            modifier = Modifier.fillMaxWidth().height(148.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            last7.forEachIndexed { index, (dayLabel, volume) ->
                val heightFraction = if (volume > 0) (volume / safeMax).coerceIn(0.07f, 1f) else 0.04f
                val isHighlight = (index == maxIdx) && volume > 0
                val barColor = if (isHighlight) Primary
                               else if (volume > 0) SurfaceContainerHighest
                               else SurfaceContainerHighest.copy(alpha = 0.35f)
                val labelText = when {
                    volume <= 0f -> ""
                    volume >= 1000f -> "${"%.1f".format(volume / 1000f)}k"
                    else -> "${volume.toInt()}"
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Değer etiketi — sabit yükseklik, her zaman aynı hizada
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = if (isHighlight) 9.sp else 8.sp
                        ),
                        color = if (isHighlight) Primary else OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.height(13.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    // Çubuk alanı: Box kalan tüm yüksekliği alır,
                    // içindeki çubuk alt hizadan fillMaxHeight(fraction) ile büyür
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(heightFraction)
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(barColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    // Gün etiketi
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = if (isHighlight) FontWeight.Black else FontWeight.Bold
                        ),
                        color = if (isHighlight) Primary else OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Weekly Progress Card
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun WeeklyProgressCard(sessions: List<WorkoutSession>) {
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    // Bu haftanın Pazartesi'si
    val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)

    // Her gün için antrenman var mı?
    val weekDays = (0..6).map { offset ->
        val date = monday.plusDays(offset.toLong())
        val hasWorkout = sessions.any { it.date == date.format(fmt) }
        val dayName = date.dayOfWeek
            .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
            .take(2)
            .uppercase()
        Triple(date, hasWorkout, dayName)
    }

    val trainedDays = weekDays.count { it.second }

    // Bu haftanın istatistikleri
    val thisWeekSessions = sessions.filter {
        val d = LocalDate.parse(it.date, fmt)
        !d.isBefore(monday) && !d.isAfter(monday.plusDays(6))
    }
    val thisWeekVolume = thisWeekSessions.sumOf { it.totalVolumeKg.toDouble() }.toFloat()
    val thisWeekSets = thisWeekSessions.sumOf { s -> s.exercises.sumOf { it.sets.size } }

    // Geçen haftanın istatistikleri (karşılaştırma)
    val lastMonday = monday.minusWeeks(1)
    val lastWeekSessions = sessions.filter {
        val d = LocalDate.parse(it.date, fmt)
        !d.isBefore(lastMonday) && !d.isAfter(lastMonday.plusDays(6))
    }
    val lastWeekVolume = lastWeekSessions.sumOf { it.totalVolumeKg.toDouble() }.toFloat()
    val lastWeekTrained = (0..6).count { offset ->
        val d = lastMonday.plusDays(offset.toLong())
        lastWeekSessions.any { it.date == d.format(fmt) }
    }

    val volumeDiffStr = when {
        lastWeekVolume <= 0f && thisWeekVolume > 0f -> "↑ ilk hafta"
        lastWeekVolume <= 0f -> "henüz veri yok"
        else -> {
            val diff = thisWeekVolume - lastWeekVolume
            val pct = (diff / lastWeekVolume * 100).toInt()
            if (pct >= 0) "↑ geçen haftadan +$pct%" else "↓ geçen haftadan $pct%"
        }
    }
    val daysDiffStr = when {
        lastWeekTrained == 0 -> ""
        trainedDays > lastWeekTrained -> " · geçen haftadan +${trainedDays - lastWeekTrained} gün"
        trainedDays < lastWeekTrained -> " · geçen haftadan ${trainedDays - lastWeekTrained} gün"
        else -> " · geçen haftayla aynı"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SurfaceContainerHigh,
                        Primary.copy(alpha = 0.08f)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column {
            // Başlık satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BU HAFTA",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = "${monday.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))} – ${monday.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}".uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
                        ),
                        color = OnSurfaceVariant
                    )
                }
                // Kaç gün antrenmanlı
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$trainedDays",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black
                        ),
                        color = Primary
                    )
                    Text(
                        text = "/7",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black
                        ),
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "GÜN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold, fontSize = 9.sp
                        ),
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7 gün göstergesi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                weekDays.forEach { (date, hasWorkout, dayName) ->
                    val isToday = date == today
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        hasWorkout -> Primary
                                        isToday -> Primary.copy(alpha = 0.15f)
                                        else -> SurfaceContainerHighest.copy(alpha = 0.6f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasWorkout) {
                                // Tamamlanan gün: ateş ikonu
                                Icon(
                                    Icons.Outlined.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = OnPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = "${date.dayOfMonth}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = LexendFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isToday) Primary else OnSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = if (hasWorkout || isToday) FontWeight.Black else FontWeight.Medium
                            ),
                            color = if (hasWorkout) Primary else if (isToday) Primary else OnSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            HorizontalDivider(color = OutlineVariant.copy(alpha = 0.20f), thickness = 1.dp)

            Spacer(modifier = Modifier.height(14.dp))

            // İstatistik satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                WeekStatItem(
                    value = "$trainedDays",
                    label = "ANTRENMAN",
                    highlight = trainedDays > 0
                )
                // Dikey ayırıcı
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(OutlineVariant.copy(alpha = 0.3f))
                )
                WeekStatItem(
                    value = if (thisWeekVolume >= 1000f) "${"%.1f".format(thisWeekVolume / 1000f)}k" else "${thisWeekVolume.toInt()}",
                    label = "KG HACİM",
                    highlight = thisWeekVolume > 0f
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(OutlineVariant.copy(alpha = 0.3f))
                )
                WeekStatItem(
                    value = "$thisWeekSets",
                    label = "TOPLAM SET",
                    highlight = thisWeekSets > 0
                )
            }

            // Karşılaştırma satırı
            if (lastWeekVolume > 0f || lastWeekTrained > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$volumeDiffStr$daysDiffStr",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    ),
                    color = OnSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun WeekStatItem(value: String, label: String, highlight: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black
            ),
            color = if (highlight) Primary else OnSurfaceVariant.copy(alpha = 0.4f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = OnSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Quick Stats Row
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun QuickStatsRow(streak: Int, prKg: Float?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerLow)
                .drawBehind {
                    drawLine(color = Primary, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 6f)
                }
                .padding(16.dp)
        ) {
            Column {
                Text(text = "STREAK", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$streak GÜN",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = LexendFamily, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic),
                    color = OnSurface
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerLow)
                .drawBehind {
                    drawLine(color = Secondary, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 6f)
                }
                .padding(16.dp)
        ) {
            Column {
                Text(text = "BUGÜN PR", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (prKg != null) "${prKg.toInt()} KG" else "— KG",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = LexendFamily, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic),
                    color = Secondary
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Welcome Banner
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun WelcomeBanner(userName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors = listOf(Primary.copy(alpha = 0.15f), Secondary.copy(alpha = 0.08f))))
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = if (userName.isNotBlank()) "HOŞ GELDİN, ${userName.uppercase()}!" else "HOŞ GELDİN!",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = LexendFamily, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
                color = Primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "GymBuddy ile fitness yolculuğuna başla.\nİlk antrenmanını kaydet ve gelişimini takip et!",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.Medium, lineHeight = 22.sp),
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTipBadge(icon = Icons.Outlined.FitnessCenter, text = "Antrenman Kaydet")
                QuickTipBadge(icon = Icons.Outlined.LocalFireDepartment, text = "Kalori Takip")
            }
        }
    }
}

@Composable
private fun QuickTipBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = OnSurface
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Bildirim Bottom Sheet
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsSheet(
    workoutState: WorkoutUiState,
    followRequests: List<FollowRequestNotif> = emptyList(),
    onAcceptRequest: (String) -> Unit = {},
    onRejectRequest: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val fmt   = DateTimeFormatter.ISO_LOCAL_DATE
    val today = LocalDate.now()

    // Bildirim öğelerini hesapla
    val todayStr        = today.format(fmt)
    val workedOutToday  = workoutState.sessions.any { it.date == todayStr }
    val streak          = workoutState.currentStreak
    val totalWorkouts   = workoutState.sessions.size

    // Bu hafta kaç antrenman?
    val weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val thisWeekCount = workoutState.sessions.count { s ->
        try {
            val d = LocalDate.parse(s.date)
            !d.isBefore(weekStart) && !d.isAfter(today)
        } catch (e: Exception) { false }
    }

    // En son PR bu hafta?
    val latestPrKg = workoutState.prTodayKg

    data class NotifItem(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val title: String,
        val body: String,
        val isAlert: Boolean = false
    )

    val items = buildList {
        if (!workedOutToday) {
            add(NotifItem(
                icon    = Icons.Outlined.FitnessCenter,
                title   = "Bugün antrenman yok",
                body    = "Henüz bugün antrenman kaydetmedin. Serinizi korumak için şimdi başla!",
                isAlert = streak > 0
            ))
        } else {
            add(NotifItem(
                icon  = Icons.Outlined.FitnessCenter,
                title = "Bugün antrenman tamamlandı 💪",
                body  = "Harika iş! Bugünkü seansını kaydettiniz."
            ))
        }
        if (streak > 1) {
            add(NotifItem(
                icon  = Icons.Outlined.LocalFireDepartment,
                title = "$streak günlük seri devam ediyor!",
                body  = "Tutarlılığın ödülü büyük. Yarın da devam et."
            ))
        }
        if (thisWeekCount > 0) {
            add(NotifItem(
                icon  = Icons.Outlined.CalendarToday,
                title = "Bu hafta $thisWeekCount antrenman",
                body  = if (thisWeekCount >= 3) "Bu hafta harika gidiyor!" else "Haftayı güçlü bitir!"
            ))
        }
        if (latestPrKg != null) {
            add(NotifItem(
                icon  = Icons.Outlined.FitnessCenter,
                title = "Bugünkü en ağır set",
                body  = "${"%.1f".format(latestPrKg)} kg ile güçlü bir seans!"
            ))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceContainer,
        dragHandle       = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OutlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            // Başlık
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "BİLDİRİMLER",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily    = LexendFamily,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = OnSurface
                    )
                    Text(
                        "Antrenman durumun",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = ManropeFamily),
                        color = OnSurfaceVariant
                    )
                }
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint     = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Takip istekleri
            if (followRequests.isNotEmpty()) {
                Text(
                    "TAKİP İSTEKLERİ",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = Secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    followRequests.forEach { req ->
                        FollowRequestCard(
                            request   = req,
                            onAccept  = { onAcceptRequest(req.fromUid) },
                            onReject  = { onRejectRequest(req.fromUid) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.20f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (items.isEmpty() && followRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            tint     = OnSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Bildirim yok",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = ManropeFamily),
                            color = OnSurfaceVariant
                        )
                    }
                }
            } else if (items.isNotEmpty()) {
                if (followRequests.isNotEmpty()) {
                    Text(
                        "ANTRENMAN",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { notif ->
                        NotifCard(notif.icon, notif.title, notif.body, notif.isAlert)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotifCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    isAlert: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isAlert) Secondary.copy(alpha = 0.08f)
                else SurfaceContainerLow
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isAlert) Secondary.copy(alpha = 0.15f)
                    else Primary.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = if (isAlert) Secondary else Primary,
                modifier           = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isAlert) Secondary else OnSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = body,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = ManropeFamily,
                    lineHeight = 18.sp
                ),
                color = OnSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Follow Request Card
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun FollowRequestCard(
    request: FollowRequestNotif,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Secondary.copy(alpha = 0.08f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (request.fromPhotoUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(request.fromPhotoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = request.fromName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                )
            } else {
                Text(
                    text = request.fromName.split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Bold
                    ),
                    color = OnSurfaceVariant
                )
            }
        }

        // İsim + metin
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.fromName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFamily, fontWeight = FontWeight.Bold
                ),
                color = OnSurface
            )
            Text(
                text = "sana takip isteği gönderdi",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = ManropeFamily),
                color = OnSurfaceVariant
            )
        }

        // Kabul / Reddet butonları
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary)
                    .clickable { onAccept() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "KABUL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Black,
                        fontSize = 9.sp, letterSpacing = 0.5.sp
                    ),
                    color = OnPrimary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { onReject() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SİL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Black,
                        fontSize = 9.sp, letterSpacing = 0.5.sp
                    ),
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Preview
// ═══════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF0E0E0E)
@Composable
fun HomeScreenPreview() {
    GymBuddyTheme {
        HomeScreen()
    }
}
