package com.example.gymbuddy.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gymbuddy.ui.theme.*

// ── Data Models ─────────────────────────────────────────────────────

data class ProfileData(
    val name: String = "Kullanıcı",
    val username: String = "@kullanici",
    val badge: String = "NEWBIE",
    val friends: String = "0",
    val totalWorkouts: Int = 0,
    val initials: String = "K",
    val profilePhotoUrl: String = ""
)

data class ProfileMenuItem(
    val icon: ImageVector,
    val label: String,
    val route: String
)

private val menuItems = listOf(
    ProfileMenuItem(Icons.Filled.PersonOutline, "Profili Düzenle", "edit_profile"),
    ProfileMenuItem(Icons.Filled.Groups, "Arkadaşlarım", "my_friends"),
    ProfileMenuItem(Icons.Filled.Restaurant, "Öğün Geçmişi", "meal_history"),
    ProfileMenuItem(Icons.Filled.History, "Antrenman Geçmişi", "workout_history"),
    ProfileMenuItem(Icons.Filled.TrackChanges, "Hedeflerim", "hedeflerim"),
    ProfileMenuItem(Icons.Filled.Tune, "Uygulama Ayarları", "app_settings")
)

// ── Main Screen ─────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    profileData: ProfileData = ProfileData(),
    onMenuItemClick: (String) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
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
            // ── Top Bar ─────────────────────────────────────────
            ProfileTopBar()

            // ── Profile Header ──────────────────────────────────
            ProfileHeader(profileData = profileData)

            Spacer(modifier = Modifier.height(28.dp))

            // ── Stats Row ───────────────────────────────────────
            StatsRow(profileData = profileData)

            Spacer(modifier = Modifier.height(28.dp))

            // ── Menu Items ──────────────────────────────────────
            MenuSection(
                items = menuItems,
                onItemClick = onMenuItemClick,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Logout Button ───────────────────────────────────
            LogoutButton(
                onClick = onLogoutClick,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // Bottom spacing for nav bar
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

// ── Top Bar ─────────────────────────────────────────────────────────

@Composable
private fun ProfileTopBar() {
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
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                fontStyle = FontStyle.Italic
            ),
            color = Primary
        )
    }
}

// ── Profile Header ──────────────────────────────────────────────────

@Composable
private fun ProfileHeader(profileData: ProfileData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar with gradient frame + rotation
        Box(contentAlignment = Alignment.Center) {
            // Gradient border frame (slightly rotated)
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .rotate(3f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Primary, Secondary),
                            start = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY),
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f)
                        )
                    )
            )

            // Inner avatar container (counter-rotated)
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(SurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                if (profileData.profilePhotoUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profileData.profilePhotoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = profileData.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = profileData.initials,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = LexendFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 40.sp
                        ),
                        color = OnSurfaceVariant
                    )
                }
            }

            // ELITE badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Primary)
                    .padding(horizontal = 16.dp, vertical = 5.dp)
            ) {
                Text(
                    text = profileData.badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp
                    ),
                    color = OnPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Name
        Text(
            text = profileData.name,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            color = OnSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Username
        Text(
            text = profileData.username,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            ),
            color = OnSurfaceVariant
        )
    }
}

// ── Stats Row ───────────────────────────────────────────────────────

@Composable
private fun StatsRow(profileData: ProfileData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            value = profileData.friends,
            label = "ARKADAŞ",
            valueColor = Primary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = "${profileData.totalWorkouts}",
            label = "ANTRENMAN",
            valueColor = Secondary,
            hasAccentBorder = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    valueColor: Color,
    hasAccentBorder: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .then(
                if (hasAccentBorder) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = Primary.copy(alpha = 0.10f),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                } else Modifier
            )
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            ),
            color = valueColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                lineHeight = 13.sp
            ),
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Menu Section ────────────────────────────────────────────────────

@Composable
private fun MenuSection(
    items: List<ProfileMenuItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLow)
    ) {
        items.forEach { item ->
            MenuItemRow(
                icon = item.icon,
                label = item.label,
                onClick = { onItemClick(item.route) }
            )
        }
    }
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = LexendFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = OnSurface,
            modifier = Modifier.weight(1f)
        )

        // Chevron
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = OutlineVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ── Logout Button ───────────────────────────────────────────────────

@Composable
private fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .drawBehind {
                drawRoundRect(
                    color = Secondary.copy(alpha = 0.20f),
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Çıkış Yap",
                tint = Secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "ÇIKIŞ YAP",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = Secondary
            )
        }
    }
}
