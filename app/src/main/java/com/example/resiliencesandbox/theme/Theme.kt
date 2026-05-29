package com.example.resiliencesandbox.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonRed,
    tertiary = TextCyberGreen,
    background = CyberDark,
    surface = DarkSurface,
    onPrimary = CyberDark,
    onSecondary = CyberDark,
    onTertiary = CyberDark,
    onBackground = MutedText,
    onSurface = MutedText
)

@Composable
fun ResilienceSandboxTheme(
  darkTheme: Boolean = true, // Force Dark Theme for Cyberpunk feel
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.background.toArgb()
      window.navigationBarColor = colorScheme.background.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
