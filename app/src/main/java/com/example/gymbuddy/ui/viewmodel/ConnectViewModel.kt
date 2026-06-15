package com.example.gymbuddy.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymbuddy.data.model.User
import com.example.gymbuddy.data.repository.UserRepository
import com.example.gymbuddy.data.repository.haversineKm
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── Yakındaki kullanıcı profili ──────────────────────────────────────

data class NearbyUserProfile(
    val uid: String,
    val fullName: String,
    val username: String,
    val fitnessFocus: String,
    val badge: String,
    val totalWorkouts: Int,
    val distanceKm: Double,
    val latitude: Double,
    val longitude: Double,
    val lastSeen: Long,
    val gymName: String = "",
    val profilePhotoUrl: String = ""
) {
    val initials: String get() = fullName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
}

// ── UI State ─────────────────────────────────────────────────────────

sealed class ConnectState {
    object PermissionRequired : ConnectState()
    object PermissionDeniedPermanently : ConnectState()
    object GymSetupRequired : ConnectState()
    data class SavingGym(val gymName: String) : ConnectState()
    object LoadingLocation : ConnectState()
    object LoadingUsers : ConnectState()
    data class Ready(
        val myLat: Double,
        val myLng: Double,
        val myGymName: String,
        val users: List<NearbyUserProfile>,
        val radiusKm: Double
    ) : ConnectState()
    data class Error(val message: String) : ConnectState()
}

data class ConnectUiState(
    val state: ConnectState = ConnectState.PermissionRequired,
    val friends: Set<String> = emptySet(),        // karşılıklı arkadaşlar
    val sentRequests: Set<String> = emptySet()    // gönderdiğim bekleyen istekler
)

// ── ViewModel ─────────────────────────────────────────────────────────

class ConnectViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    private val radiusKm = 5.0

    private var cachedGymName = ""
    private var cachedGymLat = 0.0
    private var cachedGymLng = 0.0

    // ── İzin kontrol ─────────────────────────────────────────────────
    fun checkPermissionAndLoad(context: Context, uid: String) {
        loadFollowState(uid)
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fine || coarse) {
            checkGymAndLoad(context, uid)
        } else {
            _uiState.update { it.copy(state = ConnectState.PermissionRequired) }
        }
    }

    // ── Takip durumlarını yükle ──────────────────────────────────────
    private fun loadFollowState(uid: String) {
        viewModelScope.launch {
            // Arkadaşları yükle (karşılıklı takip)
            userRepository.getFollowing(uid)
                .onSuccess { following ->
                    // Followers'ı da çek, kesişimi bul
                    val userResult = userRepository.getUser(uid)
                    val user = userResult.getOrNull()
                    val followers = user?.followers?.toSet() ?: emptySet()
                    val mutualFriends = following.intersect(followers)
                    _uiState.update { it.copy(friends = mutualFriends) }
                }
            // Gönderilmiş bekleyen istekleri yükle
            userRepository.getSentPendingRequests(uid)
                .onSuccess { sent -> _uiState.update { it.copy(sentRequests = sent) } }
        }
    }

    // ── Salon kaydı kontrol et ───────────────────────────────────────
    private fun checkGymAndLoad(context: Context, uid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(state = ConnectState.LoadingLocation) }
            try {
                val userResult = userRepository.getUser(uid)
                val user = userResult.getOrNull()

                if (user == null || user.gymName.isBlank()) {
                    _uiState.update { it.copy(state = ConnectState.GymSetupRequired) }
                } else {
                    cachedGymName = user.gymName
                    cachedGymLat = user.gymLatitude
                    cachedGymLng = user.gymLongitude
                    updateLocationAndLoadUsers(context, uid)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(state = ConnectState.Error("Profil yüklenemedi: ${e.localizedMessage}"))
                }
            }
        }
    }

    // ── Salon kaydet ─────────────────────────────────────────────────
    fun saveGym(context: Context, uid: String, gymName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(state = ConnectState.SavingGym(gymName)) }
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                val location = fusedClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .await()

                if (location == null) {
                    _uiState.update { it.copy(state = ConnectState.Error("Konum alınamadı. GPS açık mı?")) }
                    return@launch
                }

                val lat = location.latitude
                val lng = location.longitude

                userRepository.saveGym(uid, gymName, lat, lng)
                    .onSuccess {
                        cachedGymName = gymName
                        cachedGymLat = lat
                        cachedGymLng = lng
                        userRepository.updateLocation(uid, lat, lng)
                        loadNearbyUsers(uid)
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(state = ConnectState.Error("Salon kaydedilemedi: ${e.localizedMessage}"))
                        }
                    }
            } catch (e: SecurityException) {
                _uiState.update { it.copy(state = ConnectState.PermissionRequired) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(state = ConnectState.Error("Konum hatası: ${e.localizedMessage}"))
                }
            }
        }
    }

    // ── Anlık konum güncelle + yakındakileri yükle ────────────────────
    private fun updateLocationAndLoadUsers(context: Context, uid: String) {
        viewModelScope.launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                val location = fusedClient
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .await()
                if (location != null) {
                    userRepository.updateLocation(uid, location.latitude, location.longitude)
                }
            } catch (_: Exception) { }
            loadNearbyUsers(uid)
        }
    }

    // ── Yakındaki kullanıcıları yükle ─────────────────────────────────
    private suspend fun loadNearbyUsers(uid: String) {
        _uiState.update { it.copy(state = ConnectState.LoadingUsers) }

        userRepository.getNearbyUsers(uid, cachedGymLat, cachedGymLng, radiusKm)
            .onSuccess { users ->
                val profiles = users
                    .map { it.toNearbyProfile(cachedGymLat, cachedGymLng) }
                    .sortedWith(
                        compareBy<NearbyUserProfile> { it.distanceKm }
                            .thenByDescending { badgeRank(it.badge) }
                            .thenByDescending { it.totalWorkouts }
                    )
                _uiState.update {
                    it.copy(
                        state = ConnectState.Ready(
                            myLat = cachedGymLat,
                            myLng = cachedGymLng,
                            myGymName = cachedGymName,
                            users = profiles,
                            radiusKm = radiusKm
                        )
                    )
                }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(state = ConnectState.Error("Kullanıcılar yüklenemedi: ${e.localizedMessage}"))
                }
            }
    }

    // ── Takip İsteği Gönder ──────────────────────────────────────────
    fun sendFollowRequest(myUid: String, targetUid: String) {
        // Optimistic update
        _uiState.update { it.copy(sentRequests = it.sentRequests + targetUid) }
        viewModelScope.launch {
            userRepository.sendFollowRequest(myUid, targetUid)
                .onFailure {
                    // Geri al
                    _uiState.update { it.copy(sentRequests = it.sentRequests - targetUid) }
                }
        }
    }

    // ── İstek İptal Et ───────────────────────────────────────────────
    fun cancelFollowRequest(myUid: String, targetUid: String) {
        _uiState.update { it.copy(sentRequests = it.sentRequests - targetUid) }
        viewModelScope.launch {
            userRepository.cancelFollowRequest(myUid, targetUid)
        }
    }

    // ── İzin reddedildi ──────────────────────────────────────────────
    fun onPermissionDeniedPermanently() {
        _uiState.update { it.copy(state = ConnectState.PermissionDeniedPermanently) }
    }

    // ── Yenile ───────────────────────────────────────────────────────
    fun refresh(context: Context, uid: String) {
        loadFollowState(uid)
        checkGymAndLoad(context, uid)
    }

    // ── Yardımcı ──────────────────────────────────────────────────────
    private fun User.toNearbyProfile(gymLat: Double, gymLng: Double) = NearbyUserProfile(
        uid = uid,
        fullName = fullName.ifBlank { "Sporcu" },
        username = username,
        fitnessFocus = fitnessFocus.ifBlank { "Fitness" },
        badge = badge,
        totalWorkouts = totalWorkouts,
        distanceKm = haversineKm(gymLat, gymLng, gymLatitude, gymLongitude),
        latitude = gymLatitude,
        longitude = gymLongitude,
        lastSeen = lastSeen,
        gymName = gymName,
        profilePhotoUrl = profilePhotoUrl
    )

    private fun badgeRank(badge: String) = when (badge.uppercase()) {
        "ELITE" -> 2
        "PRO"   -> 1
        else    -> 0
    }
}
