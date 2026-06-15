package com.example.gymbuddy.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymbuddy.data.model.User
import com.example.gymbuddy.data.repository.UserRepository
import com.example.gymbuddy.ui.screens.home.FollowRequestNotif
import kotlinx.coroutines.launch
import com.example.gymbuddy.ui.components.BottomNavItem
import com.example.gymbuddy.ui.components.GymBottomNavBar
import com.example.gymbuddy.ui.screens.connect.ConnectScreen
import com.example.gymbuddy.ui.screens.home.HomeScreen
import com.example.gymbuddy.ui.screens.chat.ChatScreen
import com.example.gymbuddy.ui.screens.profile.EditProfileScreen
import com.example.gymbuddy.ui.screens.profile.FriendsScreen
import com.example.gymbuddy.ui.screens.profile.GoalsScreen
import com.example.gymbuddy.ui.screens.profile.MealHistoryScreen
import com.example.gymbuddy.ui.screens.profile.ProfileScreen
import com.example.gymbuddy.ui.screens.profile.ProfileData
import com.example.gymbuddy.ui.screens.profile.SettingsScreen
import com.example.gymbuddy.ui.screens.profile.WorkoutHistoryScreen
import com.example.gymbuddy.ui.screens.scan.ScanScreen
import com.example.gymbuddy.ui.viewmodel.AuthViewModel
import com.example.gymbuddy.ui.viewmodel.ChatViewModel
import com.example.gymbuddy.ui.viewmodel.ConnectViewModel
import com.example.gymbuddy.ui.viewmodel.SettingsViewModel
import com.example.gymbuddy.ui.viewmodel.WorkoutViewModel

@Composable
fun MainScreen(
    onLogout: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
    workoutViewModel: WorkoutViewModel = viewModel(),
    connectViewModel: ConnectViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    var currentTab       by remember { mutableStateOf(BottomNavItem.HOME) }
    var profileSubScreen by remember { mutableStateOf<String?>(null) }

    val authState    by authViewModel.authState.collectAsState()
    val currentUser  = authState.currentUser
    val workoutState by workoutViewModel.uiState.collectAsState()
    val chatUiState  by chatViewModel.uiState.collectAsState()

    // Takip istekleri & arkadaş sayısı
    val userRepository = remember { UserRepository() }
    var followRequests by remember { mutableStateOf<List<FollowRequestNotif>>(emptyList()) }
    var friendCount by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Pending istekleri ve arkadaş sayısını yükle
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            loadFollowRequests(uid, userRepository) { followRequests = it }
            userRepository.getFriends(uid).onSuccess { friendCount = it.size }
        }
    }

    // Alt-ekran açıkken navbar gizlenir
    val hideBottomNav = (currentTab == BottomNavItem.CHAT && chatUiState.openConversation != null) ||
                        (currentTab == BottomNavItem.PROFILE && profileSubScreen != null)

    // Profil alt-ekran geri navigasyonu
    if (profileSubScreen != null) {
        BackHandler { profileSubScreen = null }
    }

    // Tab değişince profil alt-ekranı sıfırla
    LaunchedEffect(currentTab) {
        if (currentTab != BottomNavItem.PROFILE) profileSubScreen = null
    }

    // Kullanıcı hazır olduğunda workout'ları dinlemeye başla
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            workoutViewModel.startObserving(uid)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Tab content ─────────────────────────────────────────────
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith
                        fadeOut(animationSpec = tween(200))
            },
            label = "tabContent"
        ) { tab ->
            when (tab) {
                BottomNavItem.HOME -> HomeScreen(
                    userName = currentUser?.fullName?.split(" ")?.firstOrNull() ?: "",
                    workoutState = workoutState,
                    followRequests = followRequests,
                    onAcceptRequest = { fromUid ->
                        currentUser?.uid?.let { myUid ->
                            coroutineScope.launch {
                                userRepository.acceptFollowRequest(fromUid, myUid)
                                loadFollowRequests(myUid, userRepository) { followRequests = it }
                                userRepository.getFriends(myUid).onSuccess { friendCount = it.size }
                            }
                        }
                    },
                    onRejectRequest = { fromUid ->
                        currentUser?.uid?.let { myUid ->
                            coroutineScope.launch {
                                userRepository.rejectFollowRequest(fromUid, myUid)
                                loadFollowRequests(myUid, userRepository) { followRequests = it }
                            }
                        }
                    },
                    onSaveWorkout = { date, exercises ->
                        currentUser?.uid?.let { uid ->
                            workoutViewModel.saveWorkout(uid, date, exercises)
                        }
                    },
                    onClearSaveSuccess = {
                        workoutViewModel.clearSaveSuccess()
                    }
                )
                BottomNavItem.CONNECT -> ConnectScreen(
                    currentUid = currentUser?.uid ?: "",
                    viewModel  = connectViewModel
                )
                BottomNavItem.SCAN -> ScanScreen()
                BottomNavItem.CHAT -> ChatScreen(
                    myUid          = currentUser?.uid ?: "",
                    myName         = currentUser?.fullName ?: "",
                    onConnectClick = { currentTab = BottomNavItem.CONNECT },
                    viewModel      = chatViewModel
                )
                BottomNavItem.PROFILE -> {
                    when (profileSubScreen) {
                        "edit_profile" -> EditProfileScreen(
                            currentUser   = currentUser,
                            authViewModel = authViewModel,
                            onBack        = { profileSubScreen = null }
                        )
                        "workout_history" -> WorkoutHistoryScreen(
                            workoutState = workoutState,
                            onBack       = { profileSubScreen = null }
                        )
                        "hedeflerim" -> GoalsScreen(
                            currentUser   = currentUser,
                            workoutState  = workoutState,
                            authViewModel = authViewModel,
                            onBack        = { profileSubScreen = null }
                        )
                        "my_friends" -> FriendsScreen(
                            currentUid = currentUser?.uid ?: "",
                            onBack     = { profileSubScreen = null }
                        )
                        "meal_history" -> MealHistoryScreen(
                            onBack = { profileSubScreen = null }
                        )
                        "app_settings" -> SettingsScreen(
                            currentUser      = currentUser,
                            settingsViewModel = settingsViewModel,
                            onBack           = { profileSubScreen = null },
                            onAccountDeleted = { onLogout() }
                        )
                        else -> ProfileScreen(
                            profileData     = currentUser.toProfileData(friendCount),
                            onMenuItemClick = { route -> profileSubScreen = route },
                            onLogoutClick   = onLogout
                        )
                    }
                }
            }
        }

        // ── Bottom nav bar — alt-ekran açıkken kayarak gizlenir ─────
        AnimatedVisibility(
            visible  = !hideBottomNav,
            enter    = slideInVertically(tween(220)) { it },
            exit     = slideOutVertically(tween(220)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            GymBottomNavBar(
                currentRoute   = currentTab,
                onItemSelected = { currentTab = it }
            )
        }
    }
}

// ── Extension: User → ProfileData ───────────────────────────────────
private fun User?.toProfileData(friendCount: Int = 0): ProfileData {
    if (this == null) return ProfileData()
    return ProfileData(
        name          = fullName.ifBlank { "Kullanıcı" },
        username      = username.ifBlank { "@kullanici" },
        badge         = badge.ifBlank { "NEWBIE" },
        friends       = formatCount(friendCount),
        totalWorkouts = totalWorkouts,
        initials      = fullName.split(" ")
            .filter { it.isNotBlank() }
            .map { it.first().uppercaseChar() }
            .joinToString(""),
        profilePhotoUrl = profilePhotoUrl
    )
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}k"
        else          -> "$count"
    }
}

// ── Pending follow requests yükleyici ──────────────────────────────
private suspend fun loadFollowRequests(
    uid: String,
    repo: UserRepository,
    onResult: (List<FollowRequestNotif>) -> Unit
) {
    repo.getPendingRequests(uid).onSuccess { requests ->
        val notifs = requests.map { req ->
            val user = repo.getUser(req.fromUid).getOrNull()
            FollowRequestNotif(
                fromUid      = req.fromUid,
                fromName     = user?.fullName ?: "Sporcu",
                fromPhotoUrl = user?.profilePhotoUrl ?: "",
                timestamp    = req.timestamp
            )
        }.sortedByDescending { it.timestamp }
        onResult(notifs)
    }.onFailure {
        onResult(emptyList())
    }
}
