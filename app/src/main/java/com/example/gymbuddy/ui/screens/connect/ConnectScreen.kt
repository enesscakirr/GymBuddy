package com.example.gymbuddy.ui.screens.connect

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gymbuddy.ui.theme.*
import com.example.gymbuddy.ui.viewmodel.ConnectState
import com.example.gymbuddy.ui.viewmodel.ConnectViewModel
import com.example.gymbuddy.ui.viewmodel.NearbyUserProfile
import kotlin.math.cos
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════════════════════════════
// ConnectScreen
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun ConnectScreen(
    currentUid: String = "",
    viewModel: ConnectViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // İzin launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.checkPermissionAndLoad(context, currentUid)
        } else {
            viewModel.onPermissionDeniedPermanently()
        }
    }

    // İlk açılışta izni kontrol et
    LaunchedEffect(currentUid) {
        if (currentUid.isNotBlank()) {
            viewModel.checkPermissionAndLoad(context, currentUid)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ──────────────────────────────────────────────
            ConnectTopBar(
                onRefreshClick = {
                    if (currentUid.isNotBlank()) viewModel.refresh(context, currentUid)
                }
            )

            // ── İçerik ───────────────────────────────────────────────
            when (val state = uiState.state) {

                is ConnectState.PermissionRequired -> {
                    PermissionRequestContent(
                        onRequestPermission = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )
                }

                is ConnectState.PermissionDeniedPermanently -> {
                    PermissionDeniedContent()
                }

                is ConnectState.GymSetupRequired -> {
                    GymSetupContent(
                        onSaveGym = { gymName ->
                            viewModel.saveGym(context, currentUid, gymName)
                        }
                    )
                }

                is ConnectState.SavingGym -> {
                    LoadingContent(message = "\"${state.gymName}\" kaydediliyor…")
                }

                is ConnectState.LoadingLocation -> {
                    LoadingContent(message = "Konumun alınıyor…")
                }

                is ConnectState.LoadingUsers -> {
                    LoadingContent(message = "Yakındaki sporcular aranıyor…")
                }

                is ConnectState.Ready -> {
                    ReadyContent(
                        state        = state,
                        friends      = uiState.friends,
                        sentRequests = uiState.sentRequests,
                        onSendRequest = { user ->
                            viewModel.sendFollowRequest(currentUid, user.uid)
                        },
                        onCancelRequest = { user ->
                            viewModel.cancelFollowRequest(currentUid, user.uid)
                        },
                        onRefresh = {
                            if (currentUid.isNotBlank()) viewModel.refresh(context, currentUid)
                        }
                    )
                }

                is ConnectState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = {
                            if (currentUid.isNotBlank()) viewModel.refresh(context, currentUid)
                        }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Top Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ConnectTopBar(onRefreshClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(Surface.copy(alpha = 0.70f))
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "GYMBUDDY",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily    = LexendFamily,
                fontWeight    = FontWeight.Black,
                letterSpacing = (-1).sp,
                fontStyle     = FontStyle.Italic
            ),
            color = Primary,
            modifier = Modifier.align(Alignment.Center)
        )
        IconButton(
            onClick = onRefreshClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Yenile", tint = Primary)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// İzin İsteme Ekranı
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun PermissionRequestContent(onRequestPermission: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // İkon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "YAKINDAKİ\nSPORCULAR",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    lineHeight = 36.sp
                ),
                color = OnSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Salonundaki ve yakınındaki GymBuddy kullanıcılarını görmek ve antrenman partneri bulmak için konumuna erişmemiz gerekiyor.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                ),
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Özellik listesi
            listOf(
                "Aynı salona giden sporcuları bul",
                "Haritada salon çevresindeki kullanıcılar",
                "Antrenman partneri bul ve takip et"
            ).forEach { text ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Primary)
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "KONUMU ETKİNLEŞTİR",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = OnPrimary
                )
            }

            Text(
                text = "Konumun yalnızca salonunu belirlemek için kullanılır ve hiçbir zaman üçüncü taraflarla paylaşılmaz.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = ManropeFamily,
                    lineHeight = 18.sp
                ),
                color = OnSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Salon Kurulum Ekranı
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun GymSetupContent(onSaveGym: (String) -> Unit) {
    var gymName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // İkon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Secondary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "SALONUNU\nKAYDET",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = LexendFamily,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    lineHeight = 36.sp
                ),
                color = OnSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Salonundayken aşağıya salon adını yaz ve kaydet. Mevcut konumun salon konumu olarak kaydedilecek.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                ),
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = gymName,
                onValueChange = { gymName = it },
                label = {
                    Text(
                        "Salon Adı",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Medium
                    )
                },
                placeholder = {
                    Text(
                        "ör. Gold's Gym Beşiktaş",
                        fontFamily = ManropeFamily,
                        color = OnSurfaceVariant.copy(alpha = 0.4f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceContainerHigh,
                    focusedLabelColor = Primary,
                    cursorColor = Primary,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    focusedContainerColor = SurfaceContainer,
                    unfocusedContainerColor = SurfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { onSaveGym(gymName.trim()) },
                enabled = gymName.trim().length >= 2,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Secondary,
                    disabledContainerColor = SurfaceContainerHigh
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Outlined.FitnessCenter, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SALONU KAYDET",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = if (gymName.trim().length >= 2) Background else OnSurfaceVariant
                )
            }

            Text(
                text = "Salonundayken kaydet — mevcut GPS konumun salon adresi olarak kullanılacak.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = ManropeFamily,
                    lineHeight = 18.sp
                ),
                color = OnSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// İzin Kalıcı Reddedildi
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun PermissionDeniedContent() {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Outlined.LocationOff,
                contentDescription = null,
                tint = OnSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Konum İzni Reddedildi",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = LexendFamily, fontWeight = FontWeight.Black
                ),
                color = OnSurface
            )
            Text(
                text = "Konum iznini Telefon Ayarları → Uygulama İzinleri kısmından manuel olarak verebilirsin.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFamily, lineHeight = 22.sp
                ),
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ayarlara Git", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Yükleniyor
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun LoadingContent(message: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "a"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Harita placeholder (pulse)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(SurfaceContainerLowest)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridColor = SurfaceContainerHigh.copy(alpha = 0.6f)
                for (i in 1..3) {
                    drawLine(gridColor, Offset(size.width * i / 4f, 0f), Offset(size.width * i / 4f, size.height), 2.dp.toPx())
                    drawLine(gridColor, Offset(0f, size.height * i / 4f), Offset(size.width, size.height * i / 4f), 2.dp.toPx())
                }
                drawCircle(Primary.copy(alpha = pulseAlpha * 0.3f), radius = size.minDimension * 0.3f)
                drawCircle(Primary.copy(alpha = pulseAlpha * 0.6f), radius = size.minDimension * 0.12f)
                drawCircle(Primary, radius = size.minDimension * 0.04f)
            }
        }

        // Alt panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = Primary, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFamily, fontWeight = FontWeight.Medium
                    ),
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Hata
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("!", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Secondary)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = ManropeFamily),
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                Text("Tekrar Dene", fontWeight = FontWeight.Bold, color = OnPrimary)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Hazır — Harita + Liste
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ReadyContent(
    state: ConnectState.Ready,
    friends: Set<String>,
    sentRequests: Set<String>,
    onSendRequest: (NearbyUserProfile) -> Unit,
    onCancelRequest: (NearbyUserProfile) -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Harita
        Box(modifier = Modifier.weight(0.42f)) {
            MapView(
                myLat = state.myLat,
                myLng = state.myLng,
                users = state.users,
                radiusKm = state.radiusKm
            )
            // Gradient geçişi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Background)))
            )
            // Yenile FAB
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 20.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onRefresh() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Güncelle", tint = Primary, modifier = Modifier.size(22.dp))
            }
        }

        // Alt panel — kullanıcı listesi
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxWidth()
                .offset(y = (-12).dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Background)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Salon adı chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainer)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = Primary, modifier = Modifier.size(12.dp))
                Text(
                    text = state.myGymName.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Primary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Başlık
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "SPORCULAR",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = OnSurface
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (state.users.isEmpty()) "KİMSE BULUNAMADI"
                               else "${state.users.size} KİŞİ BULUNDU",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        ),
                        color = if (state.users.isEmpty()) OnSurfaceVariant else Primary
                    )
                    Text(
                        text = "${state.radiusKm.toInt()} KM ÇAPINDA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 9.sp
                        ),
                        color = OnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.users.isEmpty()) {
                EmptyNearbyState(gymName = state.myGymName)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(state.users) { user ->
                        val isFriend = friends.contains(user.uid)
                        val isRequested = sentRequests.contains(user.uid)
                        NearbyUserCard(
                            user            = user,
                            isFriend        = isFriend,
                            isRequested     = isRequested,
                            onSendRequest   = { onSendRequest(user) },
                            onCancelRequest = { onCancelRequest(user) }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Canvas Harita — gerçek koordinatlardan marker pozisyonu
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun MapView(
    myLat: Double,
    myLng: Double,
    users: List<NearbyUserProfile>,
    radiusKm: Double
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut), repeatMode = RepeatMode.Reverse
        ), label = "s"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut), repeatMode = RepeatMode.Reverse
        ), label = "a"
    )

    // Görünür alan = ±radiusKm => ±latDelta derece
    val latSpan = (radiusKm * 2) / 111.0
    val lngSpan = (radiusKm * 2) / (111.0 * cos(Math.toRadians(myLat)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceContainerLowest)
    ) {
        // Grid çizgileri
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridColor = SurfaceContainerHigh.copy(alpha = 0.5f)
            val roadColor = SurfaceContainer.copy(alpha = 0.7f)
            for (i in 1..4) {
                drawLine(gridColor, Offset(size.width * i / 5f, 0f), Offset(size.width * i / 5f, size.height), 1.dp.toPx())
                drawLine(gridColor, Offset(0f, size.height * i / 5f), Offset(size.width, size.height * i / 5f), 1.dp.toPx())
            }
            for (i in 1..2) {
                drawLine(roadColor, Offset(size.width * i / 3f, 0f), Offset(size.width * i / 3f, size.height), 2.5.dp.toPx())
                drawLine(roadColor, Offset(0f, size.height * i / 3f), Offset(size.width, size.height * i / 3f), 2.5.dp.toPx())
            }
            // Çap çemberi
            drawCircle(
                color = Primary.copy(alpha = 0.06f),
                radius = size.minDimension * 0.45f
            )
            drawCircle(
                color = Primary.copy(alpha = 0.12f),
                radius = size.minDimension * 0.45f,
                style = Stroke(width = 1.dp.toPx())
            )
            // Pulse
            drawCircle(
                color = Primary.copy(alpha = pulseAlpha * 0.4f),
                radius = size.minDimension * 0.15f * pulseScale,
                center = Offset(size.width * 0.5f, size.height * 0.5f)
            )
        }

        // Diğer kullanıcı markerları
        users.forEach { user ->
            val dLat = user.latitude - myLat
            val dLng = user.longitude - myLng
            val relX = (0.5 + dLng / lngSpan).toFloat().coerceIn(0.05f, 0.95f)
            val relY = (0.5 - dLat / latSpan).toFloat().coerceIn(0.05f, 0.95f)

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val markerX = (relX * maxWidth.value).dp
                val markerY = (relY * maxHeight.value).dp
                UserMapMarker(
                    user = user,
                    modifier = Modifier
                        .offset(x = markerX - 20.dp, y = markerY - 20.dp)
                )
            }
        }

        // Salon markeri — merkez
        Box(modifier = Modifier.align(Alignment.Center)) {
            Canvas(modifier = Modifier.size(60.dp)) {
                drawCircle(Primary.copy(alpha = pulseAlpha * 0.25f), radius = size.minDimension * 0.5f * pulseScale)
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Primary, PrimaryContainer)))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.FitnessCenter, contentDescription = "Salon", tint = Primary, modifier = Modifier.size(20.dp))
                }
            }
            // Online nokta
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Secondary)
            )
        }
    }
}

// ── Kullanıcı harita markeri ──────────────────────────────────────────

@Composable
private fun UserMapMarker(user: NearbyUserProfile, modifier: Modifier = Modifier) {
    val markerColor = when (user.badge.uppercase()) {
        "ELITE" -> Secondary
        "PRO"   -> PrimaryContainer
        else    -> OnSurfaceVariant
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .drawBehind {
                    drawCircle(markerColor.copy(alpha = 0.25f), radius = size.minDimension / 1.4f)
                }
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(markerColor.copy(alpha = 0.7f), markerColor.copy(alpha = 0.35f)))),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.initials,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = LexendFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp
                    ),
                    color = markerColor
                )
            }
        }
        Canvas(modifier = Modifier.size(6.dp)) {
            val path = Path().apply {
                moveTo(size.width / 2, size.height)
                lineTo(0f, 0f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(path, color = markerColor.copy(alpha = 0.5f))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Kullanıcı Kartı
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun NearbyUserCard(
    user: NearbyUserProfile,
    isFriend: Boolean,
    isRequested: Boolean,
    onSendRequest: () -> Unit,
    onCancelRequest: () -> Unit
) {
    val badgeColor = when (user.badge.uppercase()) {
        "ELITE" -> Secondary
        "PRO"   -> PrimaryContainer
        else    -> Color.Transparent
    }

    val distanceText = when {
        user.distanceKm < 0.1 -> "~100 m"
        user.distanceKm < 1.0 -> "${(user.distanceKm * 1000).roundToInt()} m"
        else -> "${"%.1f".format(user.distanceKm)} km"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .drawBehind {
                if (badgeColor != Color.Transparent) {
                    drawRoundRect(
                        color = badgeColor.copy(alpha = 0.12f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        style = Stroke(1.dp.toPx())
                    )
                }
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Avatar — profil fotoğrafı veya initials
        Box {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainerHigh)
                    .drawBehind {
                        if (badgeColor != Color.Transparent) {
                            drawRoundRect(
                                color = badgeColor.copy(alpha = 0.35f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                                style = Stroke(2.dp.toPx())
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (user.profilePhotoUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user.profilePhotoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = user.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Text(
                        text = user.initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = LexendFamily, fontWeight = FontWeight.Bold
                        ),
                        color = if (badgeColor != Color.Transparent) badgeColor else OnSurfaceVariant
                    )
                }
            }
            // Badge etiketi
            if (user.badge.uppercase() != "NEWBIE" && badgeColor != Color.Transparent) {
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

        Spacer(modifier = Modifier.width(12.dp))

        // Bilgiler
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.fullName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = LexendFamily, fontWeight = FontWeight.Bold
                ),
                color = OnSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = user.fitnessFocus,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = ManropeFamily, fontWeight = FontWeight.Medium
                ),
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Primary.copy(alpha = 0.12f))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, tint = Primary, modifier = Modifier.size(9.dp))
                    Text(
                        text = distanceText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp
                        ),
                        color = Primary
                    )
                }
                Text(
                    text = "${user.totalWorkouts} antrenman",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ManropeFamily, fontWeight = FontWeight.Medium, fontSize = 9.sp
                    ),
                    color = OnSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Buton: ARKADAŞ / İSTEK GÖNDERİLDİ / TAKİP ET
        val (btnBg, btnText, btnTextColor, btnAction) = when {
            isFriend -> FollowButtonStyle(SurfaceContainerHigh, "ARKADAŞ", OnSurfaceVariant) {}
            isRequested -> FollowButtonStyle(SurfaceContainerHigh, "İSTEK GÖNDERİLDİ", OnSurfaceVariant) { onCancelRequest() }
            else -> FollowButtonStyle(Primary, "TAKİP ET", OnPrimary) { onSendRequest() }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(btnBg)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { btnAction() }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isFriend) Icons.Filled.Check else Icons.Outlined.PersonAdd,
                    contentDescription = null,
                    tint = btnTextColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = btnText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = LexendFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = btnTextColor
                )
            }
        }
    }
}

private data class FollowButtonStyle(
    val bg: Color,
    val text: String,
    val textColor: Color,
    val action: () -> Unit
)

// ═══════════════════════════════════════════════════════════════════════
// Boş liste durumu
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyNearbyState(gymName: String = "") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainer)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = OnSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(52.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Henüz Kimse Yok",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = LexendFamily, fontWeight = FontWeight.Bold),
            color = OnSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (gymName.isNotBlank())
                "$gymName çevresinde aktif GymBuddy kullanıcısı bulunamadı. Arkadaşlarını davet et!"
            else
                "5 km çapında aktif GymBuddy kullanıcısı bulunamadı. Arkadaşlarını davet et!",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = ManropeFamily, lineHeight = 18.sp),
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Preview
// ═══════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF0E0E0E)
@Composable
fun ConnectScreenPreview() {
    GymBuddyTheme {
        ConnectScreen()
    }
}
