package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.repository.AppSettingsRepository

val LocalIsDarkMode = compositionLocalOf { false }

private val LightColorScheme =
  lightColorScheme(
    primary = FrndomPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7F3FF),
    onPrimaryContainer = Color(0xFF003087),
    secondary = FrndomSecondary,
    onSecondary = Color.White,
    tertiary = FrndomTertiary,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    outline = BorderLight,
    outlineVariant = Color(0xFFE4E6EB),
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = FrndomPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFF90CAF9),
    secondary = FrndomSecondary,
    onSecondary = Color.White,
    tertiary = FrndomTertiary,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    outline = BorderDark,
    outlineVariant = BorderDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val appSettingsRepo = remember { AppSettingsRepository.getInstance(context) }
  val isDarkMode by appSettingsRepo.isDarkMode.collectAsState()

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !isDarkMode
        insetsController.isAppearanceLightNavigationBars = !isDarkMode
      }
    }
  }

  CompositionLocalProvider(
    LocalIsDarkMode provides isDarkMode
  ) {
    MaterialTheme(
      colorScheme = if (isDarkMode) DarkColorScheme else LightColorScheme,
      typography = Typography,
      content = content
    )
  }
}

