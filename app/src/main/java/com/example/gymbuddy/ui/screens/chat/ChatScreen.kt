package com.example.gymbuddy.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gymbuddy.ui.theme.*
import com.example.gymbuddy.ui.viewmodel.ChatViewModel
import com.example.gymbuddy.ui.viewmodel.ConversationUiItem
import java.text.SimpleDateFormat
import java.util.*

// ── Filtreler ────────────────────────────────────────────────────────

private enum class ChatFilter(val label: String) {
    ALL("TÜM MESAJLAR"),
    UNREAD("OKUNMAMIŞ"),
    ONLINE("ÇEVRİMİÇİ")
}

// ── Ana ekran: liste ↔ sohbet geçişini yönetir ───────────────────────

@Composable
fun ChatScreen(
    myUid: String,
    myName: String = "",
    onConnectClick: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(myUid) {
        viewModel.loadConversations(myUid, myName)
    }

    // Key olarak sadece Boolean kullanılır — openConversation içindeki mesaj listesi
    // değiştiğinde animasyon tetiklenmez, sadece null↔non-null geçişinde tetiklenir.
    val isOpen = uiState.openConversation != null

    AnimatedContent(
        targetState   = isOpen,
        transitionSpec = {
            if (targetState) {
                // Liste → Sohbet: sağdan giriş
                slideInHorizontally(tween(280)) { it } + fadeIn(tween(200)) togetherWith
                        slideOutHorizontally(tween(280)) { -it / 3 } + fadeOut(tween(150))
            } else {
                // Sohbet → Liste: sola çıkış
                slideInHorizontally(tween(280)) { -it / 3 } + fadeIn(tween(200)) togetherWith
                        slideOutHorizontally(tween(280)) { it } + fadeOut(tween(150))
            }
        },
        label = "chatNavigation"
    ) { open ->
        if (!open) {
            // ── Konuşma listesi ──────────────────────────────────
            ChatListContent(
                conversations  = uiState.conversations,
                isLoading      = uiState.isLoading,
                onConvClick    = { viewModel.openConversation(it, myUid) },
                onConnectClick = onConnectClick
            )
        } else {
            // ── Canlı sohbet ekranı ──────────────────────────────
            uiState.openConversation?.let { openConv ->
                ConversationScreen(
                    detail  = openConv,
                    myUid   = myUid,
                    onBack  = { viewModel.closeConversation() },
                    onSend  = { text -> viewModel.sendMessage(myUid, text) }
                )
            }
        }
    }
}

// ── Konuşma listesi ──────────────────────────────────────────────────

@Composable
private fun ChatListContent(
    conversations: List<ConversationUiItem>,
    isLoading: Boolean,
    onConvClick: (ConversationUiItem) -> Unit,
    onConnectClick: () -> Unit
) {
    var searchQuery    by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ChatFilter.ALL) }

    val filtered = remember(selectedFilter, searchQuery, conversations) {
        var list = conversations
        when (selectedFilter) {
            ChatFilter.UNREAD -> list = list.filter { it.unreadCount > 0 }
            ChatFilter.ONLINE -> list = list.filter { it.isOnline }
            ChatFilter.ALL    -> { /* hepsi */ }
        }
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.otherName.contains(searchQuery, ignoreCase = true) ||
                        it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Top bar
            item { ChatTopBar() }

            // Arama
            item {
                ChatSearchBar(
                    query        = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier     = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Filtreler
            item {
                FilterChipsRow(
                    selectedFilter  = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    modifier        = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Yükleniyor
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(28.dp))
                    }
                }
            } else if (filtered.isNotEmpty()) {
                item {
                    Text(
                        text  = "SOHBETLER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily   = ManropeFamily,
                            fontWeight   = FontWeight.Black,
                            fontSize     = 10.sp,
                            letterSpacing = 2.sp
                        ),
                        color    = OutlineVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                items(filtered, key = { it.conversationId }) { conv ->
                    ConversationItem(
                        conv    = conv,
                        onClick = { onConvClick(conv) }
                    )
                }
            } else {
                item {
                    EmptyChatState(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)
                    )
                }
            }

            // Yeni buddy bul
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text  = "YENİ BUDDY BUL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily   = ManropeFamily,
                        fontWeight   = FontWeight.Black,
                        fontSize     = 10.sp,
                        letterSpacing = 2.sp
                    ),
                    color    = OutlineVariant,
                    modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
                )
                FindBuddiesSection(
                    onConnectClick = onConnectClick,
                    modifier       = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ── Top bar ──────────────────────────────────────────────────────────

@Composable
private fun ChatTopBar() {
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
            text  = "GYMBUDDY",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily    = LexendFamily,
                fontWeight    = FontWeight.Black,
                letterSpacing = (-1).sp,
                fontStyle     = FontStyle.Italic
            ),
            color = Primary
        )
    }
}

// ── Arama alanı ──────────────────────────────────────────────────────

@Composable
private fun ChatSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLowest)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector        = Icons.Outlined.Search,
                contentDescription = null,
                tint               = OnSurfaceVariant,
                modifier           = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text  = "Kişi veya mesaj ara...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = ManropeFamily),
                        color = Outline
                    )
                }
                BasicTextField(
                    value         = query,
                    onValueChange = onQueryChange,
                    singleLine    = true,
                    textStyle     = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFamily, color = OnSurface
                    ),
                    cursorBrush   = SolidColor(Primary),
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Filtre chips ─────────────────────────────────────────────────────

@Composable
private fun FilterChipsRow(
    selectedFilter: ChatFilter,
    onFilterSelected: (ChatFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChatFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Primary else SurfaceContainerHigh)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onFilterSelected(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text  = filter.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily   = LexendFamily,
                        fontWeight   = FontWeight.Bold,
                        fontSize     = 10.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (isSelected) OnPrimary else OnSurfaceVariant
                )
            }
        }
    }
}

// ── Konuşma satırı ───────────────────────────────────────────────────

@Composable
private fun ConversationItem(
    conv: ConversationUiItem,
    onClick: () -> Unit
) {
    val hasUnread = conv.unreadCount > 0
    val bgColor   = if (hasUnread) SurfaceContainerLow else Surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar + online göstergesi
        Box(modifier = Modifier.size(52.dp)) {
            if (conv.otherPhotoUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(conv.otherPhotoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = conv.otherName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .drawBehind {
                            drawCircle(
                                color = if (hasUnread) PrimaryFixed.copy(alpha = 0.20f)
                                else Color.Transparent,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = conv.otherInitials,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp
                        ),
                        color = if (hasUnread) Primary else OnSurfaceVariant
                    )
                }
            }
            // Online nokta
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(bgColor)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(if (conv.isOnline) PrimaryFixed else OutlineVariant)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text     = conv.otherName,
                    style    = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color    = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text  = relativeTime(conv.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ManropeFamily,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Medium,
                        fontSize   = 11.sp
                    ),
                    color = if (hasUnread) PrimaryFixed else OnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text     = conv.lastMessage,
                    style    = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = ManropeFamily,
                        fontWeight = if (hasUnread) FontWeight.SemiBold
                                     else if (!conv.hasStarted) FontWeight.Medium
                                     else FontWeight.Normal,
                        fontSize   = 13.sp,
                        fontStyle  = if (!conv.hasStarted) FontStyle.Italic else FontStyle.Normal
                    ),
                    color    = if (!conv.hasStarted) Primary.copy(alpha = 0.7f)
                               else if (hasUnread) OnSurface
                               else OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (hasUnread) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Primary)
                            .drawBehind {
                                drawCircle(
                                    color  = Primary.copy(alpha = 0.30f),
                                    radius = size.minDimension / 1.3f
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "${conv.unreadCount}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = LexendFamily,
                                fontWeight = FontWeight.Black,
                                fontSize   = 9.sp
                            ),
                            color = OnPrimary
                        )
                    }
                }
            }
        }
    }
}

// ── Boş durum ────────────────────────────────────────────────────────

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLow)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector        = Icons.Outlined.Chat,
            contentDescription = null,
            tint               = OnSurfaceVariant.copy(alpha = 0.3f),
            modifier           = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text  = "Henüz sohbetin yok",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold
            ),
            color = OnSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text  = "Connect ekranından spor arkadaşı ekle\nve birlikte antrenman planla!",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = ManropeFamily,
                lineHeight = 22.sp
            ),
            color     = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.10f))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text  = "Aşağıdan yeni buddy bul →",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 11.sp
                ),
                color = Primary
            )
        }
    }
}

// ── Find Buddies bölümü ──────────────────────────────────────────────

@Composable
private fun FindBuddiesSection(
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .drawBehind {
                drawRoundRect(
                    color        = OutlineVariant.copy(alpha = 0.10f),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style        = Stroke(width = 1.dp.toPx())
                )
            }
            .clickable(
                indication        = null,
                interactionSource = MutableInteractionSource()
            ) { onConnectClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Filled.Map,
                contentDescription = null,
                tint               = Primary,
                modifier           = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = "Yakınındaki Sporcular",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                ),
                color = OnSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = "Harita ekranına git ve çevrenizdeki aktif sporcuları keşfet.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = ManropeFamily,
                    fontSize   = 12.sp,
                    lineHeight = 16.sp
                ),
                color = OnSurfaceVariant
            )
        }
        Icon(
            imageVector        = Icons.Outlined.Search,
            contentDescription = null,
            tint               = OnSurfaceVariant,
            modifier           = Modifier.size(18.dp)
        )
    }
}

// ── Yardımcı: göreceli zaman ─────────────────────────────────────────

private fun relativeTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now  = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000        -> "Az önce"
        diff < 3_600_000     -> "${diff / 60_000}dk"
        diff < 86_400_000    -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 7 * 86_400_000 -> SimpleDateFormat("EEE", Locale("tr")).format(Date(timestamp))
        else                 -> SimpleDateFormat("d MMM", Locale("tr")).format(Date(timestamp))
    }
}
