package com.example.gymbuddy.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymbuddy.data.model.User
import com.example.gymbuddy.data.model.WorkoutSession
import com.example.gymbuddy.ui.theme.*
import com.example.gymbuddy.ui.viewmodel.AuthViewModel
import com.example.gymbuddy.ui.viewmodel.WorkoutUiState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

// ── PR Veri Modeli ────────────────────────────────────────────────────

private data class ExercisePR(
    val name: String,
    val maxWeightKg: Float,
    val achievedDate: String,       // "2026-05-20"
    val totalSessions: Int,
    val prHistory: List<Pair<String, Float>>  // (tarih, ağırlık) — sadece yeni PR'lar
)

// ═══════════════════════════════════════════════════════════════════════
// Ana Ekran
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun GoalsScreen(
    currentUser: User?,
    workoutState: WorkoutUiState,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val goalsState by authViewModel.goalsState.collectAsState()

    // ── Hedef state'leri (mevcut değerlerle başlat) ──────────────────
    var targetWorkouts by remember(currentUser?.uid) {
        mutableStateOf(currentUser?.targetWorkoutsPerWeek?.takeIf { it > 0 } ?: 3)
    }
    var targetWeightStr by remember(currentUser?.uid) {
        mutableStateOf(
            if ((currentUser?.targetWeightKg ?: 0f) > 0f)
                currentUser!!.targetWeightKg.toInt().toString()
            else ""
        )
    }

    // ── Mevcut hafta antrenman sayısı ────────────────────────────────
    val currentWeekCount = remember(workoutState.sessions) {
        val today     = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        workoutState.sessions.count { s ->
            try {
                val d = LocalDate.parse(s.date)
                !d.isBefore(weekStart) && !d.isAfter(today)
            } catch (e: Exception) { false }
        }
    }

    // ── PR hesaplama ─────────────────────────────────────────────────
    val prs = remember(workoutState.sessions) {
        computePRs(workoutState.sessions)
    }

    var expandedPrName by remember { mutableStateOf<String?>(null) }

    // ── Başarı sonrası sıfırla ────────────────────────────────────────
    LaunchedEffect(goalsState.isSuccess) {
        if (goalsState.isSuccess) {
            authViewModel.clearGoalsState()
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
                bottom = 48.dp
            )
        ) {
            // ── Sayfa başlığı ──────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(
                        text = "HEDEFLERİM",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily  = LexendFamily,
                            fontWeight  = FontWeight.Black,
                            fontSize    = 30.sp,
                            lineHeight  = 34.sp
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = "& KİŞİSEL REKORLAR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily  = LexendFamily,
                            fontWeight  = FontWeight.Black,
                            fontSize    = 14.sp,
                            letterSpacing = 1.sp
                        ),
                        color = Primary
                    )
                }
            }

            // ── Hedef Kartı ────────────────────────────────────────
            item {
                GoalEditCard(
                    targetWorkouts   = targetWorkouts,
                    onTargetWorkouts = { targetWorkouts = it },
                    targetWeightStr  = targetWeightStr,
                    onTargetWeight   = { targetWeightStr = it },
                    currentWeekCount = currentWeekCount,
                    currentWeightKg  = currentUser?.weight ?: 0f,
                    isSaving         = goalsState.isSaving,
                    onSave           = {
                        authViewModel.saveGoals(
                            targetWorkoutsPerWeek = targetWorkouts,
                            targetWeightKg        = targetWeightStr.toFloatOrNull() ?: 0f
                        )
                    },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item { Spacer(Modifier.height(28.dp)) }

            // ── PR Bölümü başlığı ──────────────────────────────────
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint     = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text  = "KİŞİSEL REKORLAR",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily    = LexendFamily,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize      = 12.sp
                        ),
                        color = Primary
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            if (prs.isEmpty()) {
                item {
                    EmptyPRContent(
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                items(prs, key = { it.name }) { pr ->
                    PRCard(
                        pr         = pr,
                        isExpanded = expandedPrName == pr.name,
                        onToggle   = {
                            expandedPrName = if (expandedPrName == pr.name) null else pr.name
                        },
                        modifier   = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 10.dp)
                    )
                }
            }
        }

        // ── Sabit Top Bar ───────────────────────────────────────────
        GoalsTopBar(onBack)
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Top Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun GoalsTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(Surface.copy(alpha = 0.70f))
            .padding(horizontal = 4.dp)
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
                fontFamily    = LexendFamily,
                fontWeight    = FontWeight.Black,
                letterSpacing = (-1).sp,
                fontStyle     = FontStyle.Italic
            ),
            color    = Primary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Hedef Düzenleme Kartı
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun GoalEditCard(
    targetWorkouts: Int,
    onTargetWorkouts: (Int) -> Unit,
    targetWeightStr: String,
    onTargetWeight: (String) -> Unit,
    currentWeekCount: Int,
    currentWeightKg: Float,
    isSaving: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLow)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Başlık
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.Flag,
                contentDescription = null,
                tint     = Primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text  = "HEDEFLER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily    = LexendFamily,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontSize      = 10.sp
                ),
                color = OnSurfaceVariant
            )
        }

        // ── Haftalık antrenman hedefi ────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text  = "Haftalık Antrenman Sayısı",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = OnSurface
            )
            // 1-7 arası chip seçici
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..7).forEach { day ->
                    val isSelected = day == targetWorkouts
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Primary
                                else SurfaceContainerHighest
                            )
                            .clickable(
                                indication        = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onTargetWorkouts(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "$day",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = LexendFamily,
                                fontWeight = FontWeight.Black
                            ),
                            color = if (isSelected) OnPrimary else OnSurfaceVariant
                        )
                    }
                }
            }
            // İlerleme çubuğu
            val progress = (currentWeekCount.toFloat() / targetWorkouts).coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = "Bu hafta: $currentWeekCount antrenman",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ManropeFamily,
                            fontSize   = 10.sp
                        ),
                        color = OnSurfaceVariant
                    )
                    Text(
                        text  = "Hedef: $targetWorkouts",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 10.sp
                        ),
                        color = Primary
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SurfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(Primary)
                    )
                }
            }
        }

        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.4f))

        // ── Kilo hedefi ──────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.MonitorWeight,
                    contentDescription = null,
                    tint     = OnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text  = "Kilo Hedefi",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurface
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Mevcut kilo
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainer)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = if (currentWeightKg > 0f) "${currentWeightKg.toInt()} kg" else "—",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black
                        ),
                        color = OnSurface
                    )
                    Text(
                        text  = "Mevcut",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ManropeFamily,
                            fontSize   = 9.sp
                        ),
                        color = OnSurfaceVariant
                    )
                }

                Text(
                    "→",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = LexendFamily),
                    color = OnSurfaceVariant.copy(alpha = 0.4f)
                )

                // Hedef kilo input
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (targetWeightStr.isNotBlank()) Primary.copy(alpha = 0.10f)
                            else SurfaceContainer
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value         = targetWeightStr,
                            onValueChange = { v ->
                                if (v.length <= 5 && v.all { it.isDigit() || it == '.' }) {
                                    onTargetWeight(v)
                                }
                            },
                            singleLine    = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle     = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = LexendFamily,
                                fontWeight = FontWeight.Black,
                                color      = if (targetWeightStr.isNotBlank()) Primary else OnSurfaceVariant,
                                textAlign  = TextAlign.Center
                            ),
                            cursorBrush   = SolidColor(Primary),
                            modifier      = Modifier.widthIn(min = 40.dp, max = 70.dp),
                            decorationBox = { inner ->
                                if (targetWeightStr.isEmpty()) {
                                    Text(
                                        "??",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = LexendFamily,
                                            fontWeight = FontWeight.Black,
                                            color      = OnSurfaceVariant.copy(alpha = 0.4f),
                                            textAlign  = TextAlign.Center
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                inner()
                            }
                        )
                        if (targetWeightStr.isNotBlank()) {
                            Text(
                                " kg",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = ManropeFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Primary
                            )
                        }
                    }
                    Text(
                        text  = "Hedef",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ManropeFamily,
                            fontSize   = 9.sp
                        ),
                        color = OnSurfaceVariant
                    )
                }

                // Fark göster
                val targetF = targetWeightStr.toFloatOrNull()
                if (targetF != null && currentWeightKg > 0f) {
                    val diff = targetF - currentWeightKg
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text  = "${if (diff > 0) "+" else ""}${"%.1f".format(diff)} kg",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = LexendFamily,
                                fontWeight = FontWeight.Black,
                                fontSize   = 14.sp
                            ),
                            color = when {
                                diff < 0 -> Secondary
                                diff > 0 -> Primary.copy(alpha = 0.7f)
                                else     -> OnSurfaceVariant
                            }
                        )
                        Text(
                            text  = if (diff < 0) "kilo vermek" else if (diff > 0) "kilo almak" else "korumak",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = ManropeFamily,
                                fontSize   = 9.sp
                            ),
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }

        // ── Kaydet butonu ─────────────────────────────────────────
        Button(
            onClick  = onSave,
            enabled  = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(18.dp),
                    color     = OnPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text  = "HEDEFLERİ KAYDET",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PR Kartı
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun PRCard(
    pr: ExercisePR,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale("tr"))

    fun formatDate(d: String): String = try {
        LocalDate.parse(d).format(dateFmt)
    } catch (e: Exception) { d }

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
        // ── Özet satırı ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ağırlık badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text  = if (pr.maxWeightKg == pr.maxWeightKg.toInt().toFloat())
                                    "${pr.maxWeightKg.toInt()}"
                                else "${"%.1f".format(pr.maxWeightKg)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black,
                            fontSize   = 16.sp
                        ),
                        color = Primary
                    )
                    Text(
                        text  = "kg",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 9.sp
                        ),
                        color = Primary.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Bilgiler
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = pr.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color    = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    if (pr.achievedDate.isNotBlank()) {
                        Text(
                            text  = formatDate(pr.achievedDate),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = ManropeFamily,
                                fontSize   = 10.sp
                            ),
                            color = OnSurfaceVariant
                        )
                        Text("·", style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant.copy(alpha = 0.4f))
                    }
                    Text(
                        text  = "${pr.totalSessions} seans",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 10.sp
                        ),
                        color = OnSurfaceVariant
                    )
                }
                if (pr.prHistory.size > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text  = "${pr.prHistory.size} PR kırıldı",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 9.sp
                        ),
                        color = Secondary
                    )
                }
            }

            // Expand/collapse ikonu
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp
                              else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Kapat" else "Geçmişi Gör",
                tint     = OnSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        // ── PR Geçmişi (expandable) ──────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter   = expandVertically(tween(220)),
            exit    = shrinkVertically(tween(180))
        ) {
            PRHistoryContent(
                prHistory = pr.prHistory,
                formatDate = ::formatDate
            )
        }
    }
}

@Composable
private fun PRHistoryContent(
    prHistory: List<Pair<String, Float>>,
    formatDate: (String) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLow.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text  = "PR GEÇMİŞİ",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily    = LexendFamily,
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp,
                fontSize      = 9.sp
            ),
            color    = OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        prHistory.reversed().forEachIndexed { idx, (date, weight) ->
            val isLatest = idx == 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Timeline çizgisi
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isLatest) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (isLatest) Primary else SurfaceContainerHighest)
                    )
                    if (idx < prHistory.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(28.dp)
                                .background(SurfaceContainerHighest)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (idx < prHistory.lastIndex) 10.dp else 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = formatDate(date),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = ManropeFamily,
                            fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isLatest) OnSurface else OnSurfaceVariant
                    )
                    Text(
                        text  = if (weight == weight.toInt().toFloat())
                                    "${weight.toInt()} kg"
                                else "${"%.1f".format(weight)} kg",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black
                        ),
                        color = if (isLatest) Primary else OnSurfaceVariant
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Boş PR Durumu
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyPRContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint     = OnSurfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text  = "Henüz PR yok",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black
                ),
                color = OnSurface
            )
            Text(
                text  = "Antrenman kaydetmeye başladıkça kişisel rekorların otomatik takip edilir.",
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
// PR Hesaplama
// ═══════════════════════════════════════════════════════════════════════

private fun computePRs(sessions: List<WorkoutSession>): List<ExercisePR> {
    // Egzersiz bazında tüm (tarih, maks ağırlık) kayıtlarını topla
    val byExercise = mutableMapOf<String, MutableList<Pair<String, Float>>>()

    sessions.sortedBy { it.dateTimestamp }.forEach { session ->
        session.exercises.forEach { ex ->
            val key     = ex.name.trim().lowercase()
            val maxInSession = ex.sets
                .mapNotNull { it.weightKg.toFloatOrNull() }
                .maxOrNull() ?: return@forEach
            if (maxInSession > 0f) {
                byExercise.getOrPut(key) { mutableListOf() }.add(session.date to maxInSession)
            }
        }
    }

    return byExercise.mapNotNull { (key, records) ->
        // Görüntü adı (orijinal büyük/küçük harf)
        val displayName = sessions.flatMap { it.exercises }
            .firstOrNull { it.name.trim().lowercase() == key }
            ?.name?.trim()
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            ?: key

        // PR geçmişi: sadece yeni rekora ulaşılan günler
        val prHistory = mutableListOf<Pair<String, Float>>()
        var currentMax = 0f
        records.forEach { (date, w) ->
            if (w > currentMax) {
                currentMax = w
                prHistory.add(date to w)
            }
        }

        if (currentMax <= 0f) return@mapNotNull null

        val uniqueSessions = sessions.count { s ->
            s.exercises.any { it.name.trim().lowercase() == key }
        }

        ExercisePR(
            name          = displayName,
            maxWeightKg   = currentMax,
            achievedDate  = prHistory.lastOrNull()?.first ?: "",
            totalSessions = uniqueSessions,
            prHistory     = prHistory
        )
    }
    .sortedByDescending { it.totalSessions }
}
