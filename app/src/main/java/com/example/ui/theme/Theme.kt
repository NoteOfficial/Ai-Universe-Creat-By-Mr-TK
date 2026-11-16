package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK,
}

private val AiUniverseShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
  )

val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun AiUniverseTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  colorPalette: ColorPaletteTheme = ColorPaletteTheme.COSMIC_VIOLET,
  highContrast: Boolean = false,
  fontScale: FontScaleOption = FontScaleOption.NORMAL,
  reducedMotion: Boolean = false,
  content: @Composable () -> Unit,
) {
  val isDark =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
      }
    }
  }

  val baseNeu = remember(colorPalette, isDark) { paletteFor(colorPalette, isDark) }
  val neuColors =
    remember(baseNeu, highContrast, isDark) {
      if (!highContrast) {
        baseNeu
      } else if (isDark) {
        baseNeu.copy(
          background = Color(0xFF0A0B10),
          card = Color(0xFF151824),
          border = Color(0xFF7B87A6),
          secondaryText = Color(0xFFE5E7EB),
        )
      } else {
        baseNeu.copy(
          background = Color(0xFFF3F6FC),
          card = Color(0xFFFFFFFF),
          border = Color(0xFF475569),
          secondaryText = Color(0xFF1E293B),
        )
      }
    }

  val colorScheme =
    remember(neuColors, isDark) {
      if (isDark) {
        darkColorScheme(
          primary = neuColors.accent,
          onPrimary = Color.White,
          primaryContainer = neuColors.accent.copy(alpha = 0.24f),
          onPrimaryContainer = Color(0xFFF0F4FF),
          secondary = neuColors.accentSecondary,
          onSecondary = Color(0xFF062524),
          secondaryContainer = neuColors.accentSecondary.copy(alpha = 0.22f),
          onSecondaryContainer = Color(0xFFD8FFF9),
          background = neuColors.background,
          onBackground = neuColors.primaryText,
          surface = neuColors.card,
          onSurface = neuColors.primaryText,
          surfaceVariant = neuColors.surfaceInset,
          onSurfaceVariant = neuColors.secondaryText,
          outline = neuColors.border,
          error = ErrorRed,
          onError = Color.White,
        )
      } else {
        lightColorScheme(
          primary = neuColors.accent,
          onPrimary = Color.White,
          primaryContainer = neuColors.accent.copy(alpha = 0.14f),
          onPrimaryContainer = neuColors.primaryText,
          secondary = neuColors.accentSecondary,
          onSecondary = Color.White,
          secondaryContainer = neuColors.accentSecondary.copy(alpha = 0.14f),
          onSecondaryContainer = neuColors.primaryText,
          background = neuColors.background,
          onBackground = neuColors.primaryText,
          surface = neuColors.card,
          onSurface = neuColors.primaryText,
          surfaceVariant = neuColors.surfaceInset,
          onSurfaceVariant = neuColors.secondaryText,
          outline = neuColors.border,
          error = ErrorRed,
          onError = Color.White,
        )
      }
    }

  val typography = remember(fontScale) { buildAiUniverseTypography(fontScale) }

  CompositionLocalProvider(
    LocalNeuColors provides neuColors,
    LocalReducedMotion provides reducedMotion,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = typography,
      shapes = AiUniverseShapes,
      content = content,
    )
  }
}
