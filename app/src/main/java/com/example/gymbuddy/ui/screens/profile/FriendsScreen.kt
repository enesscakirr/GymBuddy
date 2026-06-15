package com.example.gymbuddy.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.outlined.GroupOff
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gymbuddy.data.model.User
import com.example.gymbuddy.data.repository.FollowRequest
import com.example.gymbuddy.data.repository.UserRepository
import com.example.gymbuddy.ui.theme.*
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════
// FriendsScreen — Gelen İstekler + Arkadaş Listesi
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun FriendsScreen(
    currentUid: String,
    onBack: () -> Unit
) {
    val repo = remember { UserRepository() }
    val scope = rememberCoroutineScope()

    var pendingRequests by remember { mutableStateOf<List<Pair<FollowRequest, User?>>>(emptyList()) }
    var friends by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Veri yükle
    fun loadData() {
        scope.launch {
            isLoading = true
            // Gelen istekler
            repo.getPendingRequests(currentUid).onSuccess { requests ->
                pendingRequests = requests.map { req ->
                    val user = repo.getUser(req.fromUid).getOrNull()
                    req to user
                }
            }
            // Arkadaşlar
            repo.getFriends(currentUid).onSuccess { f ->
                friends = f.sortedBy { it.fullName }
            }
            isLoading = false
        }
    }

    LaunchedEffect(currentUid) { loadData() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val topBarHeight = statusBarTop + 56.dp
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
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
                            text = "SPOR",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = LexendFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 30.sp,
                                lineHeight = 34.sp
                            ),
                            color = OnSurface
                        )
                        Text(
                            text = "ARKADAŞLARIM",
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

                // ── Gelen İstekler ──────────────────────────────
                if (pendingRequests.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "GELEN İSTEKLER",
                            count = pendingRequests.size,
                            color = Secondary
                        )
                    }
                    items(pendingRequests, key = { it.first.fromUid }) { (request, user) ->
                        PendingRequestCard(
                            user = user,
                            fromUid = request.fromUid,
                            onAccept = {
                                scope.launch {
                                    repo.acceptFollowRequest(request.fromUid, currentUid)
                                    loadData()
                                }
                            },
                            onReject = {
                                scope.launch {
                                    repo.rejectFollowRequest(request.fromUid, currentUid)
                                    loadData()
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                // ── Arkadaşlarım ────────────────────────────────
                item {
                    SectionHeader(
                        title = "ARKADAŞLARIM",
                        count = friends.size,
                        color = Primary
                    )
                }

                if (friends.isEmpty()) {
                    item { EmptyFriendsCard() }
                } else {
                    items(friends, key = { it.uid }) { friend ->
                        FriendCard(
                            user = friend,
                            onUnfriend = {
                                scope.launch {
                                    repo.unfriend(currentUid, friend.uid)
                                    loadData()
                                }
                            }
                        )
                    }
                }
            }
        }

        // ── Sabit Top Bar ───────────────────────────────────────────
        FriendsTopBar(onBack = onBack)
    }
}

// ── Top Bar ─────────────────────────────────────────────────────────

@Composable
private fun FriendsTopBar(onBack: () -> Unit) {
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

// ── Section Header ──────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ),
            color = color
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                ),
                color = color
            )
        }
    }
}

// ── Pending Request Card ────────────────────────────────────────────

@Composable
private fun PendingRequestCard(
    user: User?,
    fromUid: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val name = user?.fullName?.ifBlank { "Sporcu" } ?: "Sporcu"
    val initials = name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Secondary.copy(alpha = 0.06f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (!user?.profilePhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user!!.profilePhotoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Bold
                    ),
                    color = OnSurfaceVariant
                )
            }
        }

        // İsim + odak
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = LexendFamily, fontWeight = FontWeight.Bold
                ),
                color = OnSurface
            )
            if (!user?.fitnessFocus.isNullOrBlank()) {
                Text(
                    text = user!!.fitnessFocus,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = ManropeFamily, fontWeight = FontWeight.Medium
                    ),
                    color = OnSurfaceVariant
                )
            }
        }

        // Kabul butonu
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Primary)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onAccept() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(14.dp))
                Text(
                    text = "KABUL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Black,
                        fontSize = 9.sp, letterSpacing = 0.5.sp
                    ),
                    color = OnPrimary
                )
            }
        }

        // Reddet butonu
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainerHigh)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onReject() }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Reddet", tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

// ── Friend Card ─────────────────────────────────────────────────────

@Composable
private fun FriendCard(
    user: User,
    onUnfriend: () -> Unit
) {
    val name = user.fullName.ifBlank { "Sporcu" }
    val initials = name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }

    val badgeColor = when (user.badge.uppercase()) {
        "ELITE" -> Secondary
        "PRO" -> PrimaryContainer
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (user.profilePhotoUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user.profilePhotoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = LexendFamily, fontWeight = FontWeight.Bold
                        ),
                        color = badgeColor ?: OnSurfaceVariant
                    )
                }
            }
            // Badge
            if (badgeColor != null && user.badge.uppercase() != "NEWBIE") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = user.badge.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 7.sp
                        ),
                        color = Background
                    )
                }
            }
        }

        // Bilgiler
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = LexendFamily, fontWeight = FontWeight.Bold
                ),
                color = OnSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (user.fitnessFocus.isNotBlank()) {
                    Text(
                        text = user.fitnessFocus,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = ManropeFamily, fontWeight = FontWeight.Medium
                        ),
                        color = OnSurfaceVariant
                    )
                }
                Text(
                    text = "${user.totalWorkouts} antrenman",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp
                    ),
                    color = OnSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            if (user.gymName.isNotBlank()) {
                Text(
                    text = user.gymName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ManropeFamily, fontWeight = FontWeight.Medium, fontSize = 9.sp
                    ),
                    color = OnSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }
        }

        // Arkadaşlıktan çık
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainerHigh)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onUnfriend() }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PersonRemove,
                contentDescription = "Çıkar",
                tint = OnSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Empty State ─────────────────────────────────────────────────────

@Composable
private fun EmptyFriendsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Outlined.GroupOff,
            contentDescription = null,
            tint = OnSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Henüz arkadaşın yok",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = ManropeFamily, fontWeight = FontWeight.Medium
            ),
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Keşfet sayfasından sporcuları bul ve takip isteği gönder",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = ManropeFamily
            ),
            color = OnSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}
