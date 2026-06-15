package com.example.gymbuddy.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Kinetic Noir — Dark color scheme ────────────────────────────────
private val GymBuddyDarkScheme = darkColorScheme(
    primary                = Primary,
    onPrimary              = OnPrimary,
    primaryContainer       = PrimaryContainer,
    onPrimaryContainer     = OnPrimaryContainer,
    inversePrimary         = InversePrimary,
    secondary              = Secondary,
    onSecondary            = OnSecondary,
    secondaryContainer     = SecondaryContainer,
    onSecondaryContainer   = OnSecondaryContainer,
    tertiary               = Tertiary,
    onTertiary             = OnTertiary,
    tertiaryContainer      = TertiaryContainer,
    onTertiaryContainer    = OnTertiaryContainer,
    error                  = Error,
    onError                = OnError,
    errorContainer         = ErrorContainer,
    onErrorContainer       = OnErrorContainer,
    background             = Background,
    onBackground           = OnBackground,
    surface                = Surface,
    onSurface              = OnSurface,
    surfaceVariant         = SurfaceVariant,
    onSurfaceVariant       = OnSurfaceVariant,
    surfaceTint            = SurfaceTint,
    inverseSurface         = InverseSurface,
    inverseOnSurface       = InverseOnSurface,
    outline                = Outline,
    outlineVariant         = OutlineVariant,
    surfaceBright          = SurfaceBright,
    surfaceDim             = SurfaceDim,
    surfaceContainer       = SurfaceContainer,
    surfaceContainerHigh   = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceContainerLow    = SurfaceContainerLow,
    surfaceContainerLowest = SurfaceContainerLowest,
)

// ── Kinetic Light — Light color scheme ──────────────────────────────
private val GymBuddyLightScheme = lightColorScheme(
    primary                = Color(0xFF4A5E00),   // koyu lime — açık arka planda okunabilir
    onPrimary              = Color(0xFFFFFFFF),
    primaryContainer       = Color(0xFFCAFD00),
    onPrimaryContainer     = Color(0xFF3A4A00),
    inversePrimary         = Color(0xFFBEEE00),
    secondary              = Color(0xFFB02F00),   // koyu turuncu
    onSecondary            = Color(0xFFFFFFFF),
    secondaryContainer     = Color(0xFFFFDBD1),
    onSecondaryContainer   = Color(0xFF3E0A00),
    tertiary               = Color(0xFF665800),
    onTertiary             = Color(0xFFFFFFFF),
    tertiaryContainer      = Color(0xFFFCE047),
    onTertiaryContainer    = Color(0xFF483D00),
    error                  = Color(0xFFBA1A1A),
    onError                = Color(0xFFFFFFFF),
    errorContainer         = Color(0xFFFFDAD6),
    onErrorContainer       = Color(0xFF410002),
    background             = Color(0xFFF8F8F8),
    onBackground           = Color(0xFF1A1A1A),
    surface                = Color(0xFFFFFFFF),
    onSurface              = Color(0xFF1A1A1A),
    surfaceVariant         = Color(0xFFEEEEEE),
    onSurfaceVariant       = Color(0xFF555555),
    surfaceTint            = Color(0xFF4A5E00),
    inverseSurface         = Color(0xFF2E2E2E),
    inverseOnSurface       = Color(0xFFF5F5F5),
    outline                = Color(0xFF888888),
    outlineVariant         = Color(0xFFCCCCCC),
    surfaceBright          = Color(0xFFFFFFFF),
    surfaceDim             = Color(0xFFE8E8E8),
    surfaceContainer       = Color(0xFFF0F0F0),
    surfaceContainerHigh   = Color(0xFFEAEAEA),
    surfaceContainerHighest = Color(0xFFE4E4E4),
    surfaceContainerLow    = Color(0xFFF5F5F5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

@Composable
fun GymBuddyTheme(
    themeMode: String = "dark",   // "dark" | "light" | "system"
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        "light"  -> false
        "system" -> isSystemDark
        else     -> true   // "dark"
    }
    val colorScheme = if (useDark) GymBuddyDarkScheme else GymBuddyLightScheme

    // Status bar / nav bar görünürlüğü temaya göre ayarla
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars   = !useDark
                isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
