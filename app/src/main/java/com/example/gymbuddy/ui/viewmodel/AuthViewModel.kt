package com.example.gymbuddy.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymbuddy.data.model.User
import com.example.gymbuddy.data.repository.AuthRepository
import com.example.gymbuddy.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── UI States ───────────────────────────────────────────────────────

data class AuthUiState(
    val isLoading: Boolean = true,       // Uygulama başlangıcında true
    val isLoggedIn: Boolean = false,
    val isProfileComplete: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null
)

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

data class RegisterUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

data class ProfileSetupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val uploadingPhoto: Boolean = false
)

data class EditProfileUiState(
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

data class GoalsUiState(
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

// ── ViewModel ───────────────────────────────────────────────────────

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    // ── Auth State (app-level) ──────────────────────────────────────
    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    // ── Login State ─────────────────────────────────────────────────
    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    // ── Register State ──────────────────────────────────────────────
    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    // ── Profile Setup State ─────────────────────────────────────────
    private val _profileSetupState = MutableStateFlow(ProfileSetupUiState())
    val profileSetupState: StateFlow<ProfileSetupUiState> = _profileSetupState.asStateFlow()

    // ── Edit Profile State ──────────────────────────────────────────
    private val _editProfileState = MutableStateFlow(EditProfileUiState())
    val editProfileState: StateFlow<EditProfileUiState> = _editProfileState.asStateFlow()

    // ── Goals State ─────────────────────────────────────────────────
    private val _goalsState = MutableStateFlow(GoalsUiState())
    val goalsState: StateFlow<GoalsUiState> = _goalsState.asStateFlow()

    init {
        observeAuthState()
    }

    // ── Auth State Observer ─────────────────────────────────────────
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authStateFlow().collect { firebaseUser ->
                if (firebaseUser != null) {
                    // Kullanıcı giriş yapmış — profil durumunu kontrol et
                    val profileComplete = userRepository.isProfileComplete(firebaseUser.uid)
                    val userData = userRepository.getUser(firebaseUser.uid).getOrNull()

                    _authState.value = AuthUiState(
                        isLoading = false,
                        isLoggedIn = true,
                        isProfileComplete = profileComplete,
                        currentUser = userData
                    )
                } else {
                    // Kullanıcı giriş yapmamış
                    _authState.value = AuthUiState(
                        isLoading = false,
                        isLoggedIn = false,
                        isProfileComplete = false,
                        currentUser = null
                    )
                }
            }
        }
    }

    // ── Email/Password Login ────────────────────────────────────────
    fun loginWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginUiState(error = "Email ve şifre boş bırakılamaz")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginUiState(isLoading = true)

            authRepository.loginWithEmail(email, password)
                .onSuccess {
                    _loginState.value = LoginUiState(isSuccess = true)
                }
                .onFailure { e ->
                    _loginState.value = LoginUiState(
                        error = mapFirebaseError(e)
                    )
                }
        }
    }

    // ── Email/Password Register ─────────────────────────────────────
    fun registerWithEmail(fullName: String, email: String, password: String) {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            _registerState.value = RegisterUiState(error = "Tüm alanlar doldurulmalı")
            return
        }
        if (password.length < 6) {
            _registerState.value = RegisterUiState(error = "Şifre en az 6 karakter olmalı")
            return
        }

        viewModelScope.launch {
            _registerState.value = RegisterUiState(isLoading = true)

            authRepository.registerWithEmail(email, password)
                .onSuccess { firebaseUser ->
                    // Firestore'da temel kullanıcı belgesi oluştur
                    val newUser = User(
                        uid = firebaseUser.uid,
                        fullName = fullName,
                        email = email,
                        username = "@${fullName.lowercase().replace(" ", "_")}",
                        isProfileComplete = false
                    )
                    userRepository.saveUser(newUser)
                    _registerState.value = RegisterUiState(isSuccess = true)
                }
                .onFailure { e ->
                    _registerState.value = RegisterUiState(
                        error = mapFirebaseError(e)
                    )
                }
        }
    }

    // ── Google Sign-In ──────────────────────────────────────────────
    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = LoginUiState(isLoading = true)

            authRepository.loginWithGoogle(idToken)
                .onSuccess { firebaseUser ->
                    // Kullanıcı daha önce kayıt olmamışsa Firestore'a ekle
                    val existingUser = userRepository.getUser(firebaseUser.uid).getOrNull()
                    if (existingUser == null) {
                        val newUser = User(
                            uid = firebaseUser.uid,
                            fullName = firebaseUser.displayName ?: "",
                            email = firebaseUser.email ?: "",
                            username = "@${(firebaseUser.displayName ?: "user").lowercase().replace(" ", "_")}",
                            profilePhotoUrl = firebaseUser.photoUrl?.toString() ?: "",
                            isProfileComplete = false
                        )
                        userRepository.saveUser(newUser)
                    }
                    _loginState.value = LoginUiState(isSuccess = true)
                }
                .onFailure { e ->
                    _loginState.value = LoginUiState(
                        error = mapFirebaseError(e)
                    )
                }
        }
    }

    // ── Complete Profile ────────────────────────────────────────────
    fun completeProfile(
        fullName: String,
        age: String,
        height: String,
        weight: Float,
        address: String,
        fitnessFocus: String,
        photoUri: Uri?
    ) {
        val uid = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            _profileSetupState.value = ProfileSetupUiState(isLoading = true)

            try {
                // Fotoğraf yükle (varsa)
                var photoUrl = ""
                if (photoUri != null) {
                    _profileSetupState.value = ProfileSetupUiState(
                        isLoading = true,
                        uploadingPhoto = true
                    )
                    photoUrl = userRepository.uploadProfilePhoto(uid, photoUri)
                        .getOrDefault("")
                }

                // Profil bilgilerini güncelle
                val fields = mapOf(
                    "fullName" to fullName,
                    "age" to age,
                    "height" to height,
                    "weight" to weight,
                    "address" to address,
                    "fitnessFocus" to fitnessFocus,
                    "profilePhotoUrl" to photoUrl,
                    "isProfileComplete" to true,
                    "username" to "@${fullName.lowercase().replace(" ", "_")}"
                )

                userRepository.updateUserFields(uid, fields)
                    .onSuccess {
                        // Auth state'i yeniden tetikle
                        _authState.value = _authState.value.copy(isProfileComplete = true)
                        _profileSetupState.value = ProfileSetupUiState(isSuccess = true)
                    }
                    .onFailure { e ->
                        _profileSetupState.value = ProfileSetupUiState(
                            error = "Profil kaydedilemedi: ${e.localizedMessage}"
                        )
                    }
            } catch (e: Exception) {
                _profileSetupState.value = ProfileSetupUiState(
                    error = "Bir hata oluştu: ${e.localizedMessage}"
                )
            }
        }
    }

    // ── Update Profile (Edit Profile Screen) ───────────────────────
    fun updateProfile(
        fullName: String,
        username: String,
        age: String,
        height: String,
        weight: Float,
        address: String,
        fitnessFocus: String,
        photoUri: Uri?
    ) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _editProfileState.value = EditProfileUiState(isSaving = true)
            try {
                // Fotoğraf varsa yükle, yoksa mevcut URL'i koru
                var photoUrl = _authState.value.currentUser?.profilePhotoUrl ?: ""
                if (photoUri != null) {
                    photoUrl = userRepository.uploadProfilePhoto(uid, photoUri)
                        .getOrDefault(photoUrl)
                }

                val fields = mapOf(
                    "fullName"      to fullName,
                    "username"      to username,
                    "age"           to age,
                    "height"        to height,
                    "weight"        to weight,
                    "address"       to address,
                    "fitnessFocus"  to fitnessFocus,
                    "profilePhotoUrl" to photoUrl
                )

                userRepository.updateUserFields(uid, fields)
                    .onSuccess {
                        // Güncel kullanıcı verisini yeniden çek
                        val updatedUser = userRepository.getUser(uid).getOrNull()
                        _authState.update { it.copy(currentUser = updatedUser) }
                        _editProfileState.value = EditProfileUiState(isSuccess = true)
                    }
                    .onFailure { e ->
                        _editProfileState.value = EditProfileUiState(
                            error = "Kaydedilemedi: ${e.localizedMessage}"
                        )
                    }
            } catch (e: Exception) {
                _editProfileState.value = EditProfileUiState(
                    error = "Bir hata oluştu: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearEditProfileState() {
        _editProfileState.value = EditProfileUiState()
    }

    // ── Save Goals ──────────────────────────────────────────────────
    fun saveGoals(targetWorkoutsPerWeek: Int, targetWeightKg: Float) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _goalsState.value = GoalsUiState(isSaving = true)
            userRepository.updateUserFields(
                uid,
                mapOf(
                    "targetWorkoutsPerWeek" to targetWorkoutsPerWeek,
                    "targetWeightKg"        to targetWeightKg
                )
            ).onSuccess {
                val updatedUser = userRepository.getUser(uid).getOrNull()
                _authState.update { it.copy(currentUser = updatedUser) }
                _goalsState.value = GoalsUiState(isSuccess = true)
            }.onFailure { e ->
                _goalsState.value = GoalsUiState(
                    error = "Kaydedilemedi: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearGoalsState() {
        _goalsState.value = GoalsUiState()
    }

    // ── Logout ──────────────────────────────────────────────────────
    fun logout() {
        authRepository.logout()
        // State'ler sıfırlanacak — authStateFlow otomatik tetikler
        _loginState.value = LoginUiState()
        _registerState.value = RegisterUiState()
        _profileSetupState.value = ProfileSetupUiState()
    }

    // ── Clear Errors ────────────────────────────────────────────────
    fun clearLoginError() {
        _loginState.value = _loginState.value.copy(error = null)
    }

    fun clearRegisterError() {
        _registerState.value = _registerState.value.copy(error = null)
    }

    // ── Error Mapping ───────────────────────────────────────────────
    private fun mapFirebaseError(e: Throwable): String {
        return when {
            e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ||
            e.message?.contains("INVALID_EMAIL") == true ->
                "Geçersiz email veya şifre"

            e.message?.contains("EMAIL_EXISTS") == true ||
            e.message?.contains("email address is already in use") == true ->
                "Bu email zaten kayıtlı"

            e.message?.contains("WEAK_PASSWORD") == true ->
                "Şifre çok zayıf, en az 6 karakter olmalı"

            e.message?.contains("NETWORK") == true ||
            e.message?.contains("network") == true ->
                "İnternet bağlantınızı kontrol edin"

            e.message?.contains("TOO_MANY_ATTEMPTS") == true ->
                "Çok fazla deneme yaptınız, lütfen bekleyin"

            else -> e.localizedMessage ?: "Beklenmeyen bir hata oluştu"
        }
    }
}
