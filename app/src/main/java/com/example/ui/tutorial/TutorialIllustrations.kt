package com.example.ui.tutorial

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.ui.theme.LocalNeuColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TutorialVectorIllustration(
  pageIndex: Int,
  reducedMotion: Boolean,
  modifier: Modifier = Modifier,
) {
  val neu = LocalNeuColors.current
  val infiniteTransition = rememberInfiniteTransition(label = "tutorial_anim")
  val orbitAngle by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = if (reducedMotion) 60000 else 14000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "orbit",
    )
  val pulse by
    infiniteTransition.animateFloat(
      initialValue = 0.92f,
      targetValue = 1.08f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = if (reducedMotion) 8000 else 2200, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "pulse",
    )

  val accent = neu.accent
  val cyan = Color(0xFF06B6D4)
  val emerald = Color(0xFF10B981)
  val coral = Color(0xFFFF4D6D)
  val amber = Color(0xFFF59E0B)
  val violet = Color(0xFF8B7CFF)

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val r = minOf(w, h) * 0.36f

    // Subtle cosmic backdrop aura
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(accent.copy(alpha = 0.20f), Color.Transparent),
          center = Offset(cx, cy),
          radius = r * 1.45f,
        ),
      radius = r * 1.45f,
      center = Offset(cx, cy),
    )

    when (pageIndex) {
      0 -> {
        // Page 1: Futuristic AI Universe with colorful connected AI symbols
        drawCircle(
          color = accent.copy(alpha = 0.25f),
          radius = r * 0.95f,
          center = Offset(cx, cy),
          style = Stroke(width = 3f),
        )
        drawCircle(
          color = cyan.copy(alpha = 0.28f),
          radius = r * 0.65f,
          center = Offset(cx, cy),
          style = Stroke(width = 2.5f),
        )
        val nodeColors = listOf(accent, cyan, emerald, coral, amber, violet)
        nodeColors.forEachIndexed { idx, col ->
          val angleRad = ((idx * 60f) + orbitAngle) * (PI / 180.0)
          val nx = cx + (r * 0.95f) * cos(angleRad).toFloat()
          val ny = cy + (r * 0.95f) * sin(angleRad).toFloat()
          drawLine(
            color = col.copy(alpha = 0.45f),
            start = Offset(cx, cy),
            end = Offset(nx, ny),
            strokeWidth = 3f,
          )
          drawCircle(color = col, radius = 16f, center = Offset(nx, ny))
          drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 5f, center = Offset(nx, ny))
        }
        drawCircle(
          brush = Brush.linearGradient(listOf(accent, violet, cyan)),
          radius = r * 0.28f * pulse,
          center = Offset(cx, cy),
        )
      }

      1 -> {
        // Page 2: Colorful grid of different AI platform cards
        val cols = 3
        val rows = 3
        val cardW = w * 0.20f
        val cardH = h * 0.20f
        val gapX = w * 0.05f
        val gapY = h * 0.05f
        val startX = cx - (cols * cardW + (cols - 1) * gapX) / 2f
        val startY = cy - (rows * cardH + (rows - 1) * gapY) / 2f
        val palette = listOf(accent, emerald, cyan, coral, amber, violet, Color(0xFF1A73E8), Color(0xFFD97757), Color(0xFF20808D))
        var k = 0
        for (row in 0 until rows) {
          for (col in 0 until cols) {
            val x = startX + col * (cardW + gapX)
            val y = startY + row * (cardH + gapY)
            val c = palette[k % palette.size]
            drawRoundRect(
              brush = Brush.linearGradient(listOf(c, c.copy(alpha = 0.65f)), Offset(x, y), Offset(x + cardW, y + cardH)),
              topLeft = Offset(x, y),
              size = Size(cardW, cardH),
              cornerRadius = CornerRadius(18f, 18f),
            )
            drawCircle(
              color = Color.White.copy(alpha = 0.85f),
              radius = cardW * 0.18f,
              center = Offset(x + cardW * 0.32f, y + cardH * 0.4f),
            )
            drawRoundRect(
              color = Color.White.copy(alpha = 0.6f),
              topLeft = Offset(x + cardW * 0.2f, y + cardH * 0.7f),
              size = Size(cardW * 0.6f, 6f),
              cornerRadius = CornerRadius(3f, 3f),
            )
            k++
          }
        }
      }

      2 -> {
        // Page 3: AI category cards and detailed info panel
        drawRoundRect(
          color = neu.card,
          topLeft = Offset(cx - r * 1.1f, cy - r * 0.85f),
          size = Size(r * 2.2f, r * 1.7f),
          cornerRadius = CornerRadius(26f, 26f),
        )
        drawRoundRect(
          color = accent.copy(alpha = 0.45f),
          topLeft = Offset(cx - r * 1.1f, cy - r * 0.85f),
          size = Size(r * 2.2f, r * 1.7f),
          cornerRadius = CornerRadius(26f, 26f),
          style = Stroke(width = 3f),
        )
        drawCircle(
          brush = Brush.linearGradient(listOf(accent, cyan)),
          radius = r * 0.25f,
          center = Offset(cx - r * 0.62f, cy - r * 0.35f),
        )
        val badgeColors = listOf(emerald, cyan, amber, coral)
        badgeColors.forEachIndexed { i, bc ->
          val bx = cx - r * 0.85f + i * (r * 0.45f)
          drawRoundRect(
            color = bc.copy(alpha = 0.8f),
            topLeft = Offset(bx, cy + r * 0.1f),
            size = Size(r * 0.38f, r * 0.22f),
            cornerRadius = CornerRadius(12f, 12f),
          )
        }
        drawRoundRect(
          brush = Brush.horizontalGradient(listOf(accent, violet)),
          topLeft = Offset(cx - r * 0.75f, cy + r * 0.45f),
          size = Size(r * 1.5f, r * 0.26f),
          cornerRadius = CornerRadius(14f, 14f),
        )
      }

      3 -> {
        // Page 4: Multiple independent browser windows
        val configs =
          listOf(
            Triple(Offset(cx - r * 0.95f, cy - r * 0.7f), coral, 0.82f),
            Triple(Offset(cx - r * 0.25f, cy - r * 0.45f), emerald, 0.88f),
            Triple(Offset(cx - r * 0.6f, cy - r * 0.1f), accent, 0.95f),
          )
        configs.forEach { (pos, col, scale) ->
          val ww = r * 1.35f * scale
          val wh = r * 1.0f * scale
          drawRoundRect(
            color = neu.card,
            topLeft = pos,
            size = Size(ww, wh),
            cornerRadius = CornerRadius(20f, 20f),
          )
          drawRoundRect(
            color = col,
            topLeft = pos,
            size = Size(ww, wh),
            cornerRadius = CornerRadius(20f, 20f),
            style = Stroke(width = 3.5f),
          )
          drawRoundRect(
            color = col.copy(alpha = 0.22f),
            topLeft = pos,
            size = Size(ww, wh * 0.28f),
            cornerRadius = CornerRadius(20f, 20f),
          )
        }
      }

      4 -> {
        // Page 5: One workspace refreshing while others remain calm
        val leftX = cx - r * 1.15f
        val rightX = cx + r * 0.15f
        // Active refreshing workspace
        drawRoundRect(
          color = accent.copy(alpha = 0.16f),
          topLeft = Offset(leftX, cy - r * 0.65f),
          size = Size(r * 1.0f, r * 1.3f),
          cornerRadius = CornerRadius(22f, 22f),
        )
        drawRoundRect(
          color = accent,
          topLeft = Offset(leftX, cy - r * 0.65f),
          size = Size(r * 1.0f, r * 1.3f),
          cornerRadius = CornerRadius(22f, 22f),
          style = Stroke(width = 3.5f),
        )
        rotate(degrees = orbitAngle, pivot = Offset(leftX + r * 0.5f, cy)) {
          drawArc(
            color = cyan,
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(leftX + r * 0.25f, cy - r * 0.25f),
            size = Size(r * 0.5f, r * 0.5f),
            style = Stroke(width = 6f),
          )
        }
        // Calm untouched workspace
        drawRoundRect(
          color = emerald.copy(alpha = 0.14f),
          topLeft = Offset(rightX, cy - r * 0.65f),
          size = Size(r * 1.0f, r * 1.3f),
          cornerRadius = CornerRadius(22f, 22f),
        )
        drawRoundRect(
          color = emerald,
          topLeft = Offset(rightX, cy - r * 0.65f),
          size = Size(r * 1.0f, r * 1.3f),
          cornerRadius = CornerRadius(22f, 22f),
          style = Stroke(width = 3f),
        )
        drawCircle(color = emerald, radius = 16f, center = Offset(rightX + r * 0.5f, cy))
      }

      5 -> {
        // Page 6: Colorful files, folders, documents, and project cards
        val folderColors = listOf(amber, cyan, violet)
        folderColors.forEachIndexed { idx, col ->
          val fx = cx - r * 0.95f + idx * r * 0.48f
          val fy = cy - r * 0.55f + idx * r * 0.22f
          drawRoundRect(
            brush = Brush.linearGradient(listOf(col, col.copy(alpha = 0.7f))),
            topLeft = Offset(fx, fy),
            size = Size(r * 0.95f, r * 0.75f),
            cornerRadius = CornerRadius(18f, 18f),
          )
          drawRoundRect(
            color = Color.White.copy(alpha = 0.7f),
            topLeft = Offset(fx + 18f, fy + 24f),
            size = Size(r * 0.5f, 8f),
            cornerRadius = CornerRadius(4f, 4f),
          )
          drawRoundRect(
            color = Color.White.copy(alpha = 0.45f),
            topLeft = Offset(fx + 18f, fy + 44f),
            size = Size(r * 0.35f, 8f),
            cornerRadius = CornerRadius(4f, 4f),
          )
        }
      }

      6 -> {
        // Page 7: Secure API key & encrypted connection illustration
        val shieldPath =
          Path().apply {
            moveTo(cx, cy - r * 0.85f)
            lineTo(cx + r * 0.72f, cy - r * 0.45f)
            lineTo(cx + r * 0.58f, cy + r * 0.42f)
            lineTo(cx, cy + r * 0.9f)
            lineTo(cx - r * 0.58f, cy + r * 0.42f)
            lineTo(cx - r * 0.72f, cy - r * 0.45f)
            close()
          }
        drawPath(
          path = shieldPath,
          brush = Brush.linearGradient(listOf(accent.copy(alpha = 0.26f), emerald.copy(alpha = 0.26f))),
        )
        drawPath(
          path = shieldPath,
          color = emerald,
          style = Stroke(width = 4f),
        )
        drawCircle(
          color = amber,
          radius = r * 0.22f * pulse,
          center = Offset(cx, cy - r * 0.08f),
          style = Stroke(width = 6f),
        )
        drawLine(
          color = amber,
          start = Offset(cx, cy + r * 0.14f),
          end = Offset(cx, cy + r * 0.48f),
          strokeWidth = 6f,
        )
      }

      else -> {
        // Page 8: AI UNIVERSE emblem surrounded by colorful universe stars
        val colors = listOf(accent, cyan, emerald, coral, amber, violet)
        colors.forEachIndexed { idx, col ->
          val angle = (idx * 60f - orbitAngle * 0.5f) * (PI / 180.0)
          val sx = cx + (r * 0.92f) * cos(angle).toFloat()
          val sy = cy + (r * 0.92f) * sin(angle).toFloat()
          drawCircle(color = col, radius = 14f * pulse, center = Offset(sx, sy))
        }
        drawCircle(
          brush = Brush.radialGradient(listOf(cyan, accent, violet)),
          radius = r * 0.48f * pulse,
          center = Offset(cx, cy),
        )
        drawCircle(
          color = Color.White.copy(alpha = 0.45f),
          radius = r * 0.68f,
          center = Offset(cx, cy),
          style = Stroke(width = 3f),
        )
      }
    }
  }
}
