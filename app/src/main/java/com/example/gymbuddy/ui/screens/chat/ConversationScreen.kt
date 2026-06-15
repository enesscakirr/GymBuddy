package com.example.gymbuddy.ui.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymbuddy.data.model.ChatMessage
import coil.compose.AsyncImage
import com.example.gymbuddy.ui.theme.*
import com.example.gymbuddy.ui.viewmodel.ConversationDetail
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConversationScreen(
    detail: ConversationDetail,
    myUid: String,
    onBack: () -> Unit,
    onSend: (String) -> Unit   // text lokal olarak buradan gelir
) {
    BackHandler(enabled = true) { onBack() }

    // Draft text burada yaşar — ViewModel'e her harf için dokunmaz
    var draftText by remember { mutableStateOf("") }
    var tappedMessageId by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Yeni mesaj geldiğinde en alta kayar
    LaunchedEffect(detail.messages.size) {
        if (detail.messages.isNotEmpty()) {
            listState.animateScrollToItem(detail.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .imePadding()
    ) {
        // ── Header ──────────────────────────────────────────────────
        ConversationHeader(
            name     = detail.otherName,
            initials = detail.otherInitials,
            photoUrl = detail.otherPhotoUrl,
            onBack   = onBack
        )

        // ── Mesajlar ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .drawBehind {
                    val iconColor = Primary.copy(alpha = 0.045f)
                    val strokeW = 1.8.dp.toPx()
                    val iconStyle = Stroke(width = strokeW)
                    val cellSize = 48.dp.toPx()
                    val iconSize = 10.dp.toPx()

                    // Spor aletleri çizim fonksiyonları
                    fun drawDumbbell(cx: Float, cy: Float) {
                        // Bar
                        drawLine(iconColor, Offset(cx - iconSize, cy), Offset(cx + iconSize, cy), strokeW)
                        // Sol ağırlık
                        drawRect(iconColor, Offset(cx - iconSize - 3.dp.toPx(), cy - 5.dp.toPx()),
                            androidx.compose.ui.geometry.Size(4.dp.toPx(), 10.dp.toPx()), style = iconStyle)
                        // Sağ ağırlık
                        drawRect(iconColor, Offset(cx + iconSize - 1.dp.toPx(), cy - 5.dp.toPx()),
                            androidx.compose.ui.geometry.Size(4.dp.toPx(), 10.dp.toPx()), style = iconStyle)
                    }

                    fun drawKettlebell(cx: Float, cy: Float) {
                        // Gövde
                        drawCircle(iconColor, radius = 6.dp.toPx(), center = Offset(cx, cy + 3.dp.toPx()), style = iconStyle)
                        // Kulp
                        drawArc(iconColor, startAngle = 200f, sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(cx - 4.dp.toPx(), cy - 10.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(8.dp.toPx(), 10.dp.toPx()),
                            style = iconStyle)
                    }

                    fun drawHeartbeat(cx: Float, cy: Float) {
                        val path = Path().apply {
                            moveTo(cx - 10.dp.toPx(), cy)
                            lineTo(cx - 5.dp.toPx(), cy)
                            lineTo(cx - 3.dp.toPx(), cy - 6.dp.toPx())
                            lineTo(cx + 1.dp.toPx(), cy + 5.dp.toPx())
                            lineTo(cx + 4.dp.toPx(), cy - 3.dp.toPx())
                            lineTo(cx + 6.dp.toPx(), cy)
                            lineTo(cx + 10.dp.toPx(), cy)
                        }
                        drawPath(path, iconColor, style = iconStyle)
                    }

                    fun drawFlame(cx: Float, cy: Float) {
                        val path = Path().apply {
                            moveTo(cx, cy + 8.dp.toPx())
                            cubicTo(cx - 7.dp.toPx(), cy + 2.dp.toPx(),
                                cx - 4.dp.toPx(), cy - 5.dp.toPx(),
                                cx, cy - 9.dp.toPx())
                            cubicTo(cx + 4.dp.toPx(), cy - 5.dp.toPx(),
                                cx + 7.dp.toPx(), cy + 2.dp.toPx(),
                                cx, cy + 8.dp.toPx())
                        }
                        drawPath(path, iconColor, style = iconStyle)
                    }

                    val icons = listOf(::drawDumbbell, ::drawKettlebell, ::drawHeartbeat, ::drawFlame)
                    var idx = 0
                    var row = 0
                    var cy = cellSize / 2
                    while (cy < size.height + cellSize) {
                        val offsetX = if (row % 2 == 0) 0f else cellSize / 2
                        var cx = offsetX + cellSize / 2
                        while (cx < size.width + cellSize) {
                            withTransform({
                                rotate(
                                    degrees = when (idx % 4) { 0 -> -12f; 1 -> 8f; 2 -> -5f; else -> 15f },
                                    pivot = Offset(cx, cy)
                                )
                            }) {
                                icons[idx % 4](cx, cy)
                            }
                            idx++
                            cx += cellSize
                        }
                        row++
                        cy += cellSize
                    }
                }
        ) {
            if (detail.isLoadingMessages) {
                CircularProgressIndicator(
                    color = Primary,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                )
            } else if (detail.messages.isEmpty()) {
                EmptyMessagesHint(
                    name = detail.otherName,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(detail.messages) { index, message ->
                        val isMe = message.senderId == myUid || message.senderId == "me"
                        val prevMsg = detail.messages.getOrNull(index - 1)
                        val nextMsg = detail.messages.getOrNull(index + 1)

                        // Gün ayırıcı
                        val showDateDivider = prevMsg == null ||
                                !sameDay(prevMsg.timestamp, message.timestamp)
                        if (showDateDivider) {
                            DateDivider(
                                timestamp = message.timestamp,
                                modifier  = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        val isFirstInGroup = prevMsg == null ||
                                prevMsg.senderId != message.senderId ||
                                showDateDivider
                        val isLastInGroup = nextMsg == null ||
                                nextMsg.senderId != message.senderId ||
                                !sameDay(message.timestamp, nextMsg.timestamp)

                        val isTapped = tappedMessageId == message.id

                        MessageBubble(
                            message        = message,
                            isMe           = isMe,
                            isFirstInGroup = isFirstInGroup,
                            isLastInGroup  = isLastInGroup,
                            showTime       = isLastInGroup || isTapped,
                            onTap          = {
                                tappedMessageId = if (isTapped) null else message.id
                            }
                        )
                    }
                }
            }
        }

        // ── Input bar ────────────────────────────────────────────────
        MessageInputBar(
            value         = draftText,
            onValueChange = { draftText = it },
            onSend        = {
                onSend(draftText)
                draftText = ""
            }
        )
    }
}

// ── Header ───────────────────────────────────────────────────────────

@Composable
private fun ConversationHeader(
    name: String,
    initials: String,
    photoUrl: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .background(SurfaceContainer)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Geri",
                tint = Primary,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SurfaceContainerHigh)
                .drawBehind {
                    drawCircle(
                        color  = Primary.copy(alpha = 0.20f),
                        style  = Stroke(width = 1.5.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(
                    text  = initials,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily  = LexendFamily,
                        fontWeight  = FontWeight.Bold,
                        fontSize    = 15.sp
                    ),
                    color = Primary
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text  = name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = OnSurface
            )
        }
    }
}

// ── Mesaj balonu ─────────────────────────────────────────────────────

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    showTime: Boolean,
    onTap: () -> Unit = {}
) {
    val bubbleColor    = if (isMe) Primary else SurfaceContainerHigh
    val textColor      = if (isMe) OnPrimary else OnSurface

    // Köşe yuvarlatma: gönderen taraftaki alt köşeyi küçük yap
    val shape = if (isMe) {
        RoundedCornerShape(
            topStart    = 18.dp,
            topEnd      = if (isFirstInGroup) 18.dp else 6.dp,
            bottomEnd   = if (isLastInGroup) 4.dp else 6.dp,
            bottomStart = 18.dp
        )
    } else {
        RoundedCornerShape(
            topStart    = if (isFirstInGroup) 18.dp else 6.dp,
            topEnd      = 18.dp,
            bottomStart = if (isLastInGroup) 4.dp else 6.dp,
            bottomEnd   = 18.dp
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 40.dp, max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onTap() }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(
                text  = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily  = ManropeFamily,
                    fontWeight  = FontWeight.Medium,
                    fontSize    = 14.sp,
                    lineHeight  = 20.sp
                ),
                color = textColor
            )
        }

        // Saat + tik: grubun son mesajında göster
        if (showTime) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    start  = if (isMe) 0.dp else 4.dp,
                    end    = if (isMe) 4.dp else 0.dp,
                    top    = 2.dp,
                    bottom = 4.dp
                )
            ) {
                Text(
                    text  = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ManropeFamily,
                        fontSize   = 10.sp
                    ),
                    color = OnSurfaceVariant
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(3.dp))
                    MessageTicks(isRead = message.isRead)
                }
            }
        }
    }
}

// ── Mesaj tikleri (✓ iletildi, ✓✓ renkli = görüldü) ─────────────────

@Composable
private fun MessageTicks(isRead: Boolean) {
    val tickColor = if (isRead) Secondary else OnSurfaceVariant
    val text = "✓✓"
    Text(
        text  = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = ManropeFamily,
            fontSize   = 11.sp,
            fontWeight = if (isRead) FontWeight.Bold else FontWeight.Normal
        ),
        color = tickColor
    )
}

// ── Gün ayırıcı ──────────────────────────────────────────────────────

@Composable
private fun DateDivider(timestamp: Long, modifier: Modifier = Modifier) {
    Row(
        modifier           = modifier.fillMaxWidth(),
        verticalAlignment  = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceContainerHigh)
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text  = formatDate(timestamp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily  = ManropeFamily,
                    fontWeight  = FontWeight.SemiBold,
                    fontSize    = 11.sp
                ),
                color = OnSurfaceVariant
            )
        }
    }
}

// ── Boş mesaj durumu ─────────────────────────────────────────────────

@Composable
private fun EmptyMessagesHint(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier           = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text      = "👋",
            fontSize  = 40.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text  = "$name ile sohbet başlıyor",
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold
            ),
            color     = OnSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text  = "İlk mesajı sen gönder!",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = ManropeFamily
            ),
            color     = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Mesaj yazma alanı ────────────────────────────────────────────────

@Composable
private fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSend = value.isNotBlank()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceContainer)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Metin alanı
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text  = "Mesaj yaz...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFamily
                    ),
                    color = Outline
                )
            }
            BasicTextField(
                value            = value,
                onValueChange    = onValueChange,
                textStyle        = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFamily,
                    color      = OnSurface
                ),
                cursorBrush      = SolidColor(Primary),
                keyboardOptions  = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines         = 5,
                modifier         = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Gönder butonu — her zaman görünür, metin yoksa soluk
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (canSend) Primary else SurfaceContainerHigh)
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    enabled           = canSend
                ) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Gönder",
                tint               = if (canSend) OnPrimary else OnSurfaceVariant,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

// ── Yardımcı: zaman formatlama ────────────────────────────────────────

private fun formatTime(timestamp: Long): String {
    val now  = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000        -> "Az önce"
        diff < 3_600_000     -> "${diff / 60_000} dk"
        diff < 86_400_000    -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        else                 -> SimpleDateFormat("dd MMM HH:mm", Locale("tr")).format(Date(timestamp))
    }
}

private fun formatDate(timestamp: Long): String {
    val now        = System.currentTimeMillis()
    val diffDays   = (now - timestamp) / 86_400_000
    return when {
        diffDays == 0L  -> "Bugün"
        diffDays == 1L  -> "Dün"
        diffDays < 7L   -> SimpleDateFormat("EEEE", Locale("tr")).format(Date(timestamp))
        else            -> SimpleDateFormat("d MMMM yyyy", Locale("tr")).format(Date(timestamp))
    }
}

private fun sameDay(t1: Long, t2: Long): Boolean {
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return fmt.format(Date(t1)) == fmt.format(Date(t2))
}
