package com.example.gymbuddy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymbuddy.data.preferences.AppPreferences
import com.example.gymbuddy.data.preferences.dataStore
import com.example.gymbuddy.data.repository.AuthRepository
import com.example.gymbuddy.data.repository.UserRepository
import com.example.gymbuddy.notification.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── UI States ────────────────────────────────────────────────────────

data class SettingsUiState(
    // Görünüm
    val themeMode: String   = "dark",   // "dark" | "amoled" | "system"
    val weightUnit: String  = "kg",     // "kg"   | "lbs"

    // Bildirimler
    val notifWorkout: Boolean = false,
    val notifHour: Int        = 8,
    val notifMin: Int         = 0,
    val notifStreak: Boolean  = true,
    val notifFriends: Boolean = true,

    // İşlem durumu
    val isSaving: Boolean    = false,
    val successMessage: String? = null,
    val errorMessage: String?   = null
)

data class PasswordChangeState(
    val isLoading: Boolean  = false,
    val isSuccess: Boolean  = false,
    val error: String?      = null
)

data class DeleteAccountState(
    val isLoading: Boolean  = false,
    val isSuccess: Boolean  = false,
    val error: String?      = null
)

// ── ViewModel ────────────────────────────────────────────────────────

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context        = application.applicationContext
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val prefs          = AppPreferences(application.dataStore)

    // ── Settings state (DataStore'dan) ───────────────────────────────
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // ── Şifre değiştirme state ───────────────────────────────────────
    private val _passwordState = MutableStateFlow(PasswordChangeState())
    val passwordState: StateFlow<PasswordChangeState> = _passwordState.asStateFlow()

    // ── Hesap silme state ────────────────────────────────────────────
    private val _deleteState = MutableStateFlow(DeleteAccountState())
    val deleteState: StateFlow<DeleteAccountState> = _deleteState.asStateFlow()

    init {
        // DataStore'u dinle, state'e yansıt
        viewModelScope.launch {
            combine(
                prefs.themeMode,
                prefs.weightUnit,
                prefs.notifWorkout,
                prefs.notifHour,
                prefs.notifMin
            ) { theme, unit, workout, hour, min ->
                _uiState.update { it.copy(
                    themeMode    = theme,
                    weightUnit   = unit,
                    notifWorkout = workout,
                    notifHour    = hour,
                    notifMin     = min
                )}
            }.collect()
        }
        viewModelScope.launch {
            combine(prefs.notifStreak, prefs.notifFriends) { streak, friends ->
                _uiState.update { it.copy(notifStreak = streak, notifFriends = friends) }
            }.collect()
        }
    }

    // ── Görünüm ──────────────────────────────────────────────────────

    fun setThemeMode(mode: String) = viewModelScope.launch {
        prefs.setThemeMode(mode)
    }

    fun setWeightUnit(unit: String) = viewModelScope.launch {
        prefs.setWeightUnit(unit)
    }

    // ── Bildirimler ──────────────────────────────────────────────────

    fun setNotifWorkout(on: Boolean) = viewModelScope.launch {
        prefs.setNotifWorkout(on)
        if (on) {
            val state = _uiState.value
            NotificationHelper.scheduleWorkoutReminder(context, state.notifHour, state.notifMin)
        } else {
            NotificationHelper.cancelWorkoutReminder(context)
        }
    }

    fun setNotifTime(hour: Int, min: Int) = viewModelScope.launch {
        prefs.setNotifTime(hour, min)
        if (_uiState.value.notifWorkout) {
            NotificationHelper.scheduleWorkoutReminder(context, hour, min)
        }
    }

    fun setNotifStreak(on: Boolean) = viewModelScope.launch {
        prefs.setNotifStreak(on)
    }

    fun setNotifFriends(on: Boolean) = viewModelScope.launch {
        prefs.setNotifFriends(on)
    }

    // ── Gizlilik (Firestore) ─────────────────────────────────────────

    fun updatePrivacy(profileVisibility: String, locationVisible: Boolean) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            userRepository.updateUserFields(uid, mapOf(
                "profileVisibility" to profileVisibility,
                "locationVisible"   to locationVisible
            )).onSuccess {
                _uiState.update { it.copy(isSaving = false, successMessage = "Gizlilik ayarları kaydedildi") }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // ── Şifre Değiştir ───────────────────────────────────────────────

    fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            _passwordState.value = PasswordChangeState(error = "Tüm alanlar doldurulmalı")
            return
        }
        if (newPassword.length < 6) {
            _passwordState.value = PasswordChangeState(error = "Yeni şifre en az 6 karakter olmalı")
            return
        }
        viewModelScope.launch {
            _passwordState.value = PasswordChangeState(isLoading = true)
            authRepository.changePassword(currentPassword, newPassword)
                .onSuccess {
                    _passwordState.value = PasswordChangeState(isSuccess = true)
                }
                .onFailure { e ->
                    _passwordState.value = PasswordChangeState(
                        error = when {
                            e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ||
                            e.message?.contains("wrong-password") == true ->
                                "Mevcut şifre hatalı"
                            e.message?.contains("NETWORK") == true ->
                                "İnternet bağlantısını kontrol edin"
                            else -> e.localizedMessage ?: "Bir hata oluştu"
                        }
                    )
                }
        }
    }

    fun clearPasswordState() { _passwordState.value = PasswordChangeState() }

    // ── Hesabı Sil ───────────────────────────────────────────────────

    fun deleteAccount(currentPassword: String, uid: String) {
        if (currentPassword.isBlank()) {
            _deleteState.value = DeleteAccountState(error = "Şifre boş bırakılamaz")
            return
        }
        viewModelScope.launch {
            _deleteState.value = DeleteAccountState(isLoading = true)
            // Önce Firestore verisini sil, sonra Auth'u
            userRepository.deleteUser(uid)
            authRepository.deleteAccount(currentPassword)
                .onSuccess {
                    _deleteState.value = DeleteAccountState(isSuccess = true)
                }
                .onFailure { e ->
                    _deleteState.value = DeleteAccountState(
                        error = when {
                            e.message?.contains("wrong-password") == true ||
                            e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                                "Şifre hatalı"
                            else -> e.localizedMessage ?: "Hesap silinemedi"
                        }
                    )
                }
        }
    }

    fun clearDeleteState() { _deleteState.value = DeleteAccountState() }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
