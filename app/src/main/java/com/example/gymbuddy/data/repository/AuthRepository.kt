package com.example.gymbuddy.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication işlemlerini yöneten repository.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // ── Current User ────────────────────────────────────────────────
    val currentUser: FirebaseUser? get() = auth.currentUser

    val isLoggedIn: Boolean get() = auth.currentUser != null

    /**
     * Auth state değişikliklerini Flow olarak dinle.
     * Giriş/çıkış yapıldığında otomatik tetiklenir.
     */
    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // ── Email/Password Register ─────────────────────────────────────
    suspend fun registerWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Kullanıcı oluşturulamadı"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Email/Password Login ────────────────────────────────────────
    suspend fun loginWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Giriş yapılamadı"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Google Sign-In ──────────────────────────────────────────────
    suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Google ile giriş yapılamadı"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Password Reset ──────────────────────────────────────────────
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Change Password ─────────────────────────────────────────────
    /**
     * Şifre değiştirmeden önce yeniden kimlik doğrulama gereklidir.
     * 1. Mevcut şifre ile reauthenticate
     * 2. Başarılı ise yeni şifreyi güncelle
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        val user  = auth.currentUser ?: return Result.failure(Exception("Oturum bulunamadı"))
        val email = user.email        ?: return Result.failure(Exception("Email bulunamadı"))
        return try {
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Delete Account ──────────────────────────────────────────────
    /**
     * Hesabı tamamen sil.
     * Firebase, silmeden önce reauthentication gerektirir.
     */
    suspend fun deleteAccount(currentPassword: String): Result<Unit> {
        val user  = auth.currentUser ?: return Result.failure(Exception("Oturum bulunamadı"))
        val email = user.email        ?: return Result.failure(Exception("Email bulunamadı"))
        return try {
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Auth Provider Check ────────────────────────────────────────
    val isGoogleUser: Boolean
        get() = auth.currentUser?.providerData?.any { it.providerId == "google.com" } == true

    val isPasswordUser: Boolean
        get() = auth.currentUser?.providerData?.any { it.providerId == "password" } == true

    // ── Delete Account (Google users — no password needed) ─────────
    suspend fun deleteAccountGoogle(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Oturum bulunamadı"))
        return try {
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Logout ──────────────────────────────────────────────────────
    fun logout() {
        auth.signOut()
    }
}
