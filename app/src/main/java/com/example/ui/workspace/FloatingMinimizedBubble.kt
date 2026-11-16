package com.example.ui.workspace

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.local.WorkspaceEntity
import com.example.data.model.AiPlatform
import com.example.ui.components.AiBrandBadge
import com.example.ui.localization.LocalUiStrings
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalNeuColors
import com.example.ui.theme.LocalReducedMotion
import kotlin.math.roundToInt

@Composable
fun FloatingMinimizedOrbsOverlay(
  minimizedWorkspaces: List<WorkspaceEntity>,
  platforms: List<AiPlatform>,
  onRestoreWorkspace: (String) -> Unit,
  onCloseWorkspace: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (minimizedWorkspaces.isEmpty()) return

  val neu = LocalNeuColors.current
  val strings = LocalUiStrings.current
  val reducedMotion = LocalReducedMotion.current
  val density = LocalDensity.current

  var expanded by remember(minimizedWorkspaces.size) { mutableStateOf(false) }

  BoxWithConstraints(
    modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    val orbSizePx = with(density) { 62.dp.toPx() }
    val marginPx = with(density) { 14.dp.toPx() }
    val maxX = (constraints.maxWidth.toFloat() - orbSizePx - marginPx).coerceAtLeast(marginPx)
    val maxY = (constraints.maxHeight.toFloat() - orbSizePx - with(density) { 92.dp.toPx() }).coerceAtLeast(marginPx)

    var offsetX by remember { mutableFloatStateOf(maxX) }
    var offsetY by remember { mutableFloatStateOf(maxY * 0.72f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(maxX, maxY) {
      offsetX = offsetX.coerceIn(marginPx, maxX)
      offsetY = offsetY.coerceIn(marginPx, maxY)
    }

    val animatedX by
      animateFloatAsState(
        targetValue = offsetX,
        animationSpec =
          if (isDragging || reducedMotion) snap()
          else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "orb_x",
      )
    val animatedY by
      animateFloatAsState(
        targetValue = offsetY,
        animationSpec =
          if (isDragging || reducedMotion) snap()
          else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "orb_y",
      )

    val leadWs = minimizedWorkspaces.first()
    val leadAi = platforms.find { it.id == leadWs.aiId }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by
      animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.94f else 1f,
        animationSpec =
          if (reducedMotion) snap()
          else tween(durationMillis = 135, easing = FastOutSlowInEasing),
        label = "orb_press",
      )

    val isOnRightHalf = animatedX > (constraints.maxWidth / 2f)

    // Expanded Mini-Drawer of Circular Orbs (when multiple workspaces are minimized)
    AnimatedVisibility(
      visible = expanded && minimizedWorkspaces.size > 1,
      enter =
        if (reducedMotion) fadeIn(tween(0))
        else fadeIn(tween(180, easing = FastOutSlowInEasing)) +
          scaleIn(initialScale = 0.92f, animationSpec = tween(200, easing = FastOutSlowInEasing)),
      exit =
        if (reducedMotion) fadeOut(tween(0))
        else fadeOut(tween(140, easing = FastOutSlowInEasing)) +
          scaleOut(targetScale = 0.92f, animationSpec = tween(150, easing = FastOutSlowInEasing)),
      modifier =
        Modifier.align(if (isOnRightHalf) Alignment.BottomEnd else Alignment.BottomStart)
          .padding(horizontal = 16.dp, vertical = 104.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(26.dp),
        color = neu.card.copy(alpha = 0.96f),
        tonalElevation = 10.dp,
        shadowElevation = 12.dp,
        modifier =
          Modifier.border(1.5.dp, neu.accent.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
            .widthIn(max = 260.dp),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              String.format(strings.minimizedCountFmt, minimizedWorkspaces.size),
              style = MaterialTheme.typography.labelLarge,
              color = neu.primaryText,
              fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { expanded = false }, modifier = Modifier.size(26.dp)) {
              Icon(Icons.Rounded.Close, contentDescription = "Collapse", modifier = Modifier.size(16.dp))
            }
          }
          minimizedWorkspaces.forEach { ws ->
            val ai = platforms.find { it.id == ws.aiId }
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(18.dp))
                  .background(neu.surfaceInset)
                  .clickable {
                    expanded = false
                    onRestoreWorkspace(ws.id)
                  }
                  .padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              if (ai != null) {
                AiBrandBadge(platform = ai, size = 36.dp, isCircular = true)
              }
              Text(
                text = ws.title,
                style = MaterialTheme.typography.labelLarge,
                color = neu.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              IconButton(
                onClick = { onCloseWorkspace(ws.id) },
                modifier = Modifier.size(28.dp),
              ) {
                Icon(
                  Icons.Rounded.Close,
                  contentDescription = "Close ${ws.title}",
                  tint = ErrorRed,
                  modifier = Modifier.size(15.dp),
                )
              }
            }
          }
        }
      }
    }

    // Main Draggable Circular Floating Orb
    Box(
      modifier =
        Modifier.offset { IntOffset(animatedX.roundToInt(), animatedY.roundToInt()) }
          .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
          }
          .pointerInput(maxX, maxY) {
            detectDragGestures(
              onDragStart = { isDragging = true },
              onDragEnd = {
                isDragging = false
                // Snap cleanly to nearest horizontal edge
                offsetX = if (offsetX + orbSizePx / 2f < constraints.maxWidth / 2f) marginPx else maxX
              },
              onDragCancel = { isDragging = false },
              onDrag = { change, dragAmount ->
                change.consume()
                offsetX = (offsetX + dragAmount.x).coerceIn(marginPx, maxX)
                offsetY = (offsetY + dragAmount.y).coerceIn(marginPx, maxY)
              },
            )
          }
          .testTag("floating_minimized_orb")
    ) {
      val ringBrush =
        Brush.linearGradient(
          listOf(
            leadAi?.primaryColor ?: neu.accent,
            leadAi?.secondaryColor ?: neu.accentSecondary,
          )
        )

      Box(
        modifier =
          Modifier.size(62.dp)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(neu.card, CircleShape)
            .border(2.5.dp, ringBrush, CircleShape)
            .clickable(
              interactionSource = interactionSource,
              indication = ripple(bounded = true),
            ) {
              if (minimizedWorkspaces.size == 1) {
                onRestoreWorkspace(leadWs.id)
              } else {
                expanded = !expanded
              }
            },
        contentAlignment = Alignment.Center,
      ) {
        if (leadAi != null) {
          AiBrandBadge(platform = leadAi, size = 50.dp, isCircular = true)
        } else {
          Text(
            text = leadWs.aiName.take(2).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = neu.primaryText,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      // Close badge or multi-count badge on corner
      if (minimizedWorkspaces.size > 1) {
        Box(
          modifier =
            Modifier.align(Alignment.TopEnd)
              .size(22.dp)
              .clip(CircleShape)
              .background(neu.accent)
              .border(1.5.dp, Color.White, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "${minimizedWorkspaces.size}",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
          )
        }
      } else {
        Box(
          modifier =
            Modifier.align(Alignment.TopEnd)
              .size(22.dp)
              .clip(CircleShape)
              .background(ErrorRed)
              .border(1.5.dp, Color.White, CircleShape)
              .clickable { onCloseWorkspace(leadWs.id) },
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Rounded.Close,
            contentDescription = "Close minimized ${leadWs.aiName}",
            tint = Color.White,
            modifier = Modifier.size(13.dp),
          )
        }
      }
    }
  }
}
