package com.example.gymbuddy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymbuddy.navigation.AppNavigation
import com.example.gymbuddy.notification.NotificationHelper
import com.example.gymbuddy.ui.theme.GymBuddyTheme
import com.example.gymbuddy.ui.viewmodel.AuthViewModel
import com.example.gymbuddy.ui.viewmodel.SettingsViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var credentialManager: CredentialManager

    // Web client ID from google-services.json (client_type: 3)
    companion object {
        private const val WEB_CLIENT_ID =
            "1074022174967-maf38gr2ra25ncu4gpngg9l8sc1p6lm0.apps.googleusercontent.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannels(this)
        credentialManager = CredentialManager.create(this)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()
            val authViewModel: AuthViewModel = viewModel()

            GymBuddyTheme(themeMode = settingsState.themeMode) {
                AppNavigation(
                    authViewModel = authViewModel,
                    onGoogleSignIn = { googleSignIn(authViewModel) }
                )
            }
        }
    }

    private fun googleSignIn(authViewModel: AuthViewModel) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity
                )
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    authViewModel.loginWithGoogle(googleIdTokenCredential.idToken)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.d("GoogleSignIn", "Kullanıcı iptal etti")
            } catch (e: Exception) {
                Log.e("GoogleSignIn", "Google Sign-In hatası: ${e.message}", e)
            }
        }
    }
}
