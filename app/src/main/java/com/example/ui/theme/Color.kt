package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ColorPaletteTheme(
  val previewPrimary: Color,
  val previewSecondary: Color,
) {
  COSMIC_VIOLET(Color(0xFF4F46E5), Color(0xFF06B6D4)),
  OCEAN_AURORA(Color(0xFF0284C7), Color(0xFF10B981)),
  SUNSET_CORAL(Color(0xFFE11D48), Color(0xFFF59E0B)),
  EMERALD_CYBER(Color(0xFF059669), Color(0xFF14B8A6)),
  ROYAL_AMETHYST(Color(0xFF7C3AED), Color(0xFFF59E0B)),
}

// Legacy compatibility constants mapped to Cosmic Violet
val LightBackground = Color(0xFFECF1FA)
val LightCard = Color(0xFFFAFCFF)
val LightSurfaceVariant = Color(0xFFE2E9F7)
val LightPrimaryText = Color(0xFF121629)
val LightSecondaryText = Color(0xFF465068)
val LightAccent = Color(0xFF4F46E5)
val LightAccentSecondary = Color(0xFF06B6D4)
val LightBorder = Color(0xFFCDD8EC)
val LightHighlightShadow = Color(0xFFFFFFFF)
val LightDarkShadow = Color(0xFFB8C4D4)

val DarkBackground = Color(0xFF131522)
val DarkCard = Color(0xFF1E2236)
val DarkSurfaceVariant = Color(0xFF191C2B)
val DarkPrimaryText = Color(0xFFF8FAFC)
val DarkSecondaryText = Color(0xFFB4BED3)
val DarkAccent = Color(0xFF818CF8)
val DarkAccentSecondary = Color(0xFF22D3EE)
val DarkBorder = Color(0xFF343B59)
val DarkHighlightShadow = Color(0xFF2E323D)
val DarkDarkShadow = Color(0xFF0D0E12)

// Semantic Status Colors
val SuccessGreen = Color(0xFF059669)
val WarningAmber = Color(0xFFD97706)
val ErrorRed = Color(0xFFDC2626)
val InfoCyan = Color(0xFF0891B2)

// Curated vibrant icon colors for Quick Actions & visual accents
val VibrantPaletteAccents =
  listOf(
    Color(0xFF4F46E5), // Indigo
    Color(0xFF0284C7), // Sky Blue
    Color(0xFF059669), // Emerald
    Color(0xFFD97706), // Amber
    Color(0xFFE11D48), // Rose
    Color(0xFF7C3AED), // Violet
    Color(0xFF0D9488), // Teal
    Color(0xFFEA580C), // Orange
  )

@Immutable
data class NeuColors(
  val isDark: Boolean,
  val background: Color,
  val card: Color,
  val surfaceInset: Color,
  val primaryText: Color,
  val secondaryText: Color,
  val accent: Color,
  val accentSecondary: Color,
  val border: Color,
  val lightShadow: Color,
  val darkShadow: Color,
  val badgeTint: Color = accent.copy(alpha = 0.14f),
)

fun paletteFor(theme: ColorPaletteTheme, isDark: Boolean): NeuColors =
  when (theme) {
    ColorPaletteTheme.COSMIC_VIOLET ->
      if (isDark) {
        NeuColors(
          isDark = true,
          background = Color(0xFF121423),
          card = Color(0xFF1D2036),
          surfaceInset = Color(0xFF171A2B),
          primaryText = Color(0xFFF8FAFC),
          secondaryText = Color(0xFFB3BBD4),
          accent = Color(0xFF818CF8),
          accentSecondary = Color(0xFF22D3EE),
          border = Color(0xFF33395B),
          lightShadow = Color(0xFF2A2E4B),
          darkShadow = Color(0xFF0B0C16),
        )
      } else {
        NeuColors(
          isDark = false,
          background = Color(0xFFECF0FA),
          card = Color(0xFFFAFBFD),
          surfaceInset = Color(0xFFE0E7F6),
          primaryText = Color(0xFF121629),
          secondaryText = Color(0xFF475069),
          accent = Color(0xFF4F46E5),
          accentSecondary = Color(0xFF06B6D4),
          border = Color(0xFFCCD6EE),
          lightShadow = Color(0xFFFFFFFF),
          darkShadow = Color(0xFFB7C2D9),
        )
      }

    ColorPaletteTheme.OCEAN_AURORA ->
      if (isDark) {
        NeuColors(
          isDark = true,
          background = Color(0xFF0D1824),
          card = Color(0xFF162738),
          surfaceInset = Color(0xFF11202E),
          primaryText = Color(0xFFF0F9FF),
          secondaryText = Color(0xFFA5C3D8),
          accent = Color(0xFF38BDF8),
          accentSecondary = Color(0xFF34D399),
          border = Color(0xFF26415C),
          lightShadow = Color(0xFF20374E),
          darkShadow = Color(0xFF081018),
        )
      } else {
        NeuColors(
          isDark = false,
          background = Color(0xFFE7F3FA),
          card = Color(0xFFF8FCFF),
          surfaceInset = Color(0xFFD9ECF8),
          primaryText = Color(0xFF0D1F2D),
          secondaryText = Color(0xFF3D5A70),
          accent = Color(0xFF0284C7),
          accentSecondary = Color(0xFF10B981),
          border = Color(0xFFBDDCF0),
          lightShadow = Color(0xFFFFFFFF),
          darkShadow = Color(0xFFB0C9DA),
        )
      }

    ColorPaletteTheme.SUNSET_CORAL ->
      if (isDark) {
        NeuColors(
          isDark = true,
          background = Color(0xFF1F1219),
          card = Color(0xFF2E1C26),
          surfaceInset = Color(0xFF26161F),
          primaryText = Color(0xFFFFF5F7),
          secondaryText = Color(0xFFD7B3C0),
          accent = Color(0xFFFB7185),
          accentSecondary = Color(0xFFFBBF24),
          border = Color(0xFF4C2D3D),
          lightShadow = Color(0xFF3C2531),
          darkShadow = Color(0xFF140B10),
        )
      } else {
        NeuColors(
          isDark = false,
          background = Color(0xFFFCEEEF),
          card = Color(0xFFFFFAFB),
          surfaceInset = Color(0xFFF9E0E3),
          primaryText = Color(0xFF2B1218),
          secondaryText = Color(0xFF6B424D),
          accent = Color(0xFFE11D48),
          accentSecondary = Color(0xFFF59E0B),
          border = Color(0xFFF0C7CE),
          lightShadow = Color(0xFFFFFFFF),
          darkShadow = Color(0xFFDABBC1),
        )
      }

    ColorPaletteTheme.EMERALD_CYBER ->
      if (isDark) {
        NeuColors(
          isDark = true,
          background = Color(0xFF0D1D18),
          card = Color(0xFF152D25),
          surfaceInset = Color(0xFF11241E),
          primaryText = Color(0xFFECFDF5),
          secondaryText = Color(0xFFA2CFC0),
          accent = Color(0xFF34D399),
          accentSecondary = Color(0xFF2DD4BF),
          border = Color(0xFF254C3F),
          lightShadow = Color(0xFF1E3E33),
          darkShadow = Color(0xFF08120E),
        )
      } else {
        NeuColors(
          isDark = false,
          background = Color(0xFFE7F7F0),
          card = Color(0xFFF7FEFA),
          surfaceInset = Color(0xFFD8F1E5),
          primaryText = Color(0xFF0E2920),
          secondaryText = Color(0xFF3B6356),
          accent = Color(0xFF059669),
          accentSecondary = Color(0xFF0D9488),
          border = Color(0xFFB7E2CE),
          lightShadow = Color(0xFFFFFFFF),
          darkShadow = Color(0xFFAED1C1),
        )
      }

    ColorPaletteTheme.ROYAL_AMETHYST ->
      if (isDark) {
        NeuColors(
          isDark = true,
          background = Color(0xFF191226),
          card = Color(0xFF261C3A),
          surfaceInset = Color(0xFF201731),
          primaryText = Color(0xFFFAF5FF),
          secondaryText = Color(0xFFC5B6DE),
          accent = Color(0xFFA78BFA),
          accentSecondary = Color(0xFFFBBF24),
          border = Color(0xFF423163),
          lightShadow = Color(0xFF352750),
          darkShadow = Color(0xFF100B19),
        )
      } else {
        NeuColors(
          isDark = false,
          background = Color(0xFFF2ECFC),
          card = Color(0xFFFCF9FF),
          surfaceInset = Color(0xFFE7DDF9),
          primaryText = Color(0xFF1E1333),
          secondaryText = Color(0xFF564375),
          accent = Color(0xFF7C3AED),
          accentSecondary = Color(0xFFD97706),
          border = Color(0xFFD4C4F0),
          lightShadow = Color(0xFFFFFFFF),
          darkShadow = Color(0xFFC3B5DC),
        )
      }
  }

val LightNeuColors = paletteFor(ColorPaletteTheme.COSMIC_VIOLET, isDark = false)
val DarkNeuColors = paletteFor(ColorPaletteTheme.COSMIC_VIOLET, isDark = true)

val LocalNeuColors = staticCompositionLocalOf { LightNeuColors }
