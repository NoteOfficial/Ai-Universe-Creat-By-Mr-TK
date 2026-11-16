package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SpaceGroteskFontFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
val PlusJakartaFontFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val JetBrainsMonoFontFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))

enum class FontScaleOption(val factor: Float, val label: String) {
  SMALL(0.90f, "Small"),
  NORMAL(1.0f, "Normal"),
  LARGE(1.15f, "Large"),
}

fun buildAiUniverseTypography(scale: FontScaleOption = FontScaleOption.NORMAL): Typography {
  val f = scale.factor
  return Typography(
    displaySmall =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (28f * f).sp,
        lineHeight = (34f * f).sp,
        letterSpacing = (-0.5).sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (24f * f).sp,
        lineHeight = (30f * f).sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (20f * f).sp,
        lineHeight = (26f * f).sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (18f * f).sp,
        lineHeight = (24f * f).sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (16f * f).sp,
        lineHeight = (22f * f).sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (14f * f).sp,
        lineHeight = (20f * f).sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (15f * f).sp,
        lineHeight = (22f * f).sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (13.5f * f).sp,
        lineHeight = (19f * f).sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (12f * f).sp,
        lineHeight = (16f * f).sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (13f * f).sp,
        lineHeight = (18f * f).sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (11.5f * f).sp,
        lineHeight = (15f * f).sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (10.5f * f).sp,
        lineHeight = (14f * f).sp,
      ),
  )
}

val Typography = buildAiUniverseTypography()
