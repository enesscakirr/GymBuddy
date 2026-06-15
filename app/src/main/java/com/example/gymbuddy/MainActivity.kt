package com.example.gymbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import com.example.gymbuddy.navigation.AppNavigation
import com.example.gymbuddy.ui.theme.GymBuddyTheme
import com.example.gymbuddy.notification.NotificationHelper
import com.example.gymbuddy.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannels(this)
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()

            GymBuddyTheme(themeMode = settingsState.themeMode) {
                AppNavigation()
            }
        }
    }
}
