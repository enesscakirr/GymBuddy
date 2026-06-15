package com.example.gymbuddy.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymbuddy.ui.screens.auth.CompleteProfileScreen
import com.example.gymbuddy.ui.screens.auth.LoginScreen
import com.example.gymbuddy.ui.screens.auth.RegisterScreen
import com.example.gymbuddy.ui.screens.main.MainScreen
import com.example.gymbuddy.ui.theme.Background
import com.example.gymbuddy.ui.theme.PrimaryContainer
import com.example.gymbuddy.ui.viewmodel.AuthViewModel

// ── Route definitions ───────────────────────────────────────────────
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val COMPLETE_PROFILE = "complete_profile"
    const val MAIN = "main"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val loginState by authViewModel.loginState.collectAsState()
    val registerState by authViewModel.registerState.collectAsState()
    val profileSetupState by authViewModel.profileSetupState.collectAsState()

    // ── Reactive navigation based on auth state ─────────────────
    LaunchedEffect(authState.isLoading, authState.isLoggedIn, authState.isProfileComplete) {
        if (authState.isLoading) return@LaunchedEffect

        val currentRoute = navController.currentBackStackEntry?.destination?.route

        when {
            // Kullanıcı giriş yapmamış → Login'e
            !authState.isLoggedIn -> {
                if (currentRoute != Routes.LOGIN && currentRoute != Routes.REGISTER) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            // Giriş yapmış ama profil eksik → CompleteProfile'a
            !authState.isProfileComplete -> {
                if (currentRoute != Routes.COMPLETE_PROFILE) {
                    navController.navigate(Routes.COMPLETE_PROFILE) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            // Her şey tamam → Ana sayfaya
            else -> {
                if (currentRoute != Routes.MAIN) {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    // ── Login başarılı olduğunda ─────────────────────────────────
    LaunchedEffect(loginState.isSuccess) {
        if (loginState.isSuccess) {
            // authState observer otomatik olarak yönlendirecek
        }
    }

    // ── Register başarılı olduğunda ─────────────────────────────
    LaunchedEffect(registerState.isSuccess) {
        if (registerState.isSuccess) {
            navController.navigate(Routes.COMPLETE_PROFILE) {
                popUpTo(Routes.REGISTER) { inclusive = true }
            }
        }
    }

    // ── Profile setup başarılı olduğunda ────────────────────────
    LaunchedEffect(profileSetupState.isSuccess) {
        if (profileSetupState.isSuccess) {
            navController.navigate(Routes.MAIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                initialOffsetX = { it / 3 },
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                targetOffsetX = { it / 3 },
                animationSpec = tween(300)
            )
        }
    ) {
        // ── Splash / Loading ────────────────────────────────────────
        composable(Routes.SPLASH) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryContainer)
            }
        }

        // ── Login Screen ────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginClick = { email, password ->
                    authViewModel.loginWithEmail(email, password)
                },
                onGoogleSignInClick = {
                    // Google Sign-In, Activity'den tetiklenmeli
                    // Şimdilik placeholder — credential manager entegrasyonu aşağıda
                },
                onForgotPasswordClick = {
                    // TODO: Forgot Password ekranı
                },
                onSignUpClick = {
                    navController.navigate(Routes.REGISTER)
                },
                isLoading = loginState.isLoading,
                errorMessage = loginState.error,
                onErrorDismiss = { authViewModel.clearLoginError() }
            )
        }

        // ── Register Screen ─────────────────────────────────────────
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterClick = { fullName, email, password ->
                    authViewModel.registerWithEmail(fullName, email, password)
                },
                onLoginClick = {
                    navController.popBackStack()
                },
                isLoading = registerState.isLoading,
                errorMessage = registerState.error,
                onErrorDismiss = { authViewModel.clearRegisterError() }
            )
        }

        // ── Complete Profile Screen ─────────────────────────────────
        composable(Routes.COMPLETE_PROFILE) {
            CompleteProfileScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onLogoutClick = {
                    authViewModel.logout()
                },
                onSaveAndContinue = { fullName, age, height, weight, address, focus, photoUri ->
                    authViewModel.completeProfile(
                        fullName = fullName,
                        age = age,
                        height = height,
                        weight = weight,
                        address = address,
                        fitnessFocus = focus,
                        photoUri = photoUri
                    )
                },
                isLoading = profileSetupState.isLoading,
                errorMessage = profileSetupState.error
            )
        }

        // ── Main Screen (Home + Bottom Nav) ─────────────────────────
        composable(Routes.MAIN) {
            MainScreen(
                onLogout = {
                    authViewModel.logout()
                }
            )
        }
    }
}
