package com.example.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val CardPressSpec =
  tween<Float>(
    durationMillis = 140,
    easing = FastOutSlowInEasing,
  )

private val ButtonPressSpec =
  tween<Float>(
    durationMillis = 135,
    easing = FastOutSlowInEasing,
  )

/**
 * Hardware-accelerated GPU smooth press feedback modifier with zero overshoot/stutter.
 */
fun Modifier.springBounce(
  interactionSource: MutableInteractionSource,
  pressedScale: Float = 0.972f,
  enabled: Boolean = true,
): Modifier = composed {
  val reducedMotion = LocalReducedMotion.current
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (enabled && isPressed && !reducedMotion) pressedScale else 1f,
      animationSpec = if (reducedMotion) snap() else ButtonPressSpec,
      label = "smooth_press_scale",
    )
  this.graphicsLayer {
    scaleX = scale
    scaleY = scale
  }
}

@Composable
fun NeuCard(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 20.dp,
  onClick: (() -> Unit)? = null,
  selected: Boolean = false,
  elevated: Boolean = true,
  contentPadding: PaddingValues = PaddingValues(14.dp),
  content: @Composable BoxScope.() -> Unit,
) {
  val neu = LocalNeuColors.current
  val reducedMotion = LocalReducedMotion.current
  val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
  val borderColor =
    if (selected) neu.accent.copy(alpha = 0.85f)
    else neu.border.copy(alpha = if (neu.isDark) 0.72f else 0.90f)
  val containerColor =
    if (selected) {
      neu.accent.copy(alpha = if (neu.isDark) 0.18f else 0.08f)
    } else {
      neu.card
    }

  val shadowDp =
    when {
      !elevated -> 0.dp
      selected -> 4.dp
      neu.isDark -> 2.5.dp
      else -> 2.dp
    }

  if (onClick != null) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by
      animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.985f else 1f,
        animationSpec = if (reducedMotion) snap() else CardPressSpec,
        label = "neu_card_scale",
      )
    Surface(
      modifier =
        modifier.graphicsLayer {
          scaleX = scale
          scaleY = scale
        },
      shape = shape,
      color = containerColor,
      shadowElevation = shadowDp,
      border = BorderStroke(width = if (selected) 1.5.dp else 1.dp, color = borderColor),
    ) {
      Box(
        modifier =
          Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(color = neu.accent),
            onClick = onClick,
          )
            .padding(contentPadding),
        content = content,
      )
    }
  } else {
    Surface(
      modifier = modifier,
      shape = shape,
      color = containerColor,
      shadowElevation = shadowDp,
      border = BorderStroke(width = if (selected) 1.5.dp else 1.dp, color = borderColor),
    ) {
      Box(modifier = Modifier.padding(contentPadding), content = content)
    }
  }
}

@Composable
fun NeuInsetSurface(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 16.dp,
  contentPadding: PaddingValues = PaddingValues(12.dp),
  content: @Composable BoxScope.() -> Unit,
) {
  val neu = LocalNeuColors.current
  val shape: Shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
  Surface(
    modifier = modifier,
    shape = shape,
    color = neu.surfaceInset,
    border = BorderStroke(1.dp, neu.border.copy(alpha = 0.75f)),
  ) {
    Box(modifier = Modifier.padding(contentPadding), content = content)
  }
}

@Composable
fun NeuIconButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color? = null,
  size: Dp = 46.dp,
  content: @Composable BoxScope.() -> Unit,
) {
  val neu = LocalNeuColors.current
  val bg = containerColor ?: neu.card
  val interactionSource = remember { MutableInteractionSource() }

  Surface(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .size(size)
        .springBounce(interactionSource, pressedScale = 0.94f),
    shape = CircleShape,
    color = bg,
    shadowElevation = if (neu.isDark) 3.dp else 2.dp,
    border = BorderStroke(1.dp, neu.border.copy(alpha = 0.85f)),
  ) {
    Box(
      modifier =
        Modifier.clip(CircleShape)
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = neu.accent),
            onClick = onClick,
          ),
      contentAlignment = Alignment.Center,
      content = content,
    )
  }
}

@Composable
fun SpringButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: Shape = RoundedCornerShape(14.dp),
  colors: ButtonColors = ButtonDefaults.buttonColors(),
  contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
  content: @Composable RowScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Button(
    onClick = onClick,
    modifier = modifier.springBounce(interactionSource, pressedScale = 0.972f, enabled = enabled),
    enabled = enabled,
    shape = shape,
    colors = colors,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content,
  )
}

@Composable
fun SpringOutlinedButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: Shape = RoundedCornerShape(14.dp),
  colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
  contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
  content: @Composable RowScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  OutlinedButton(
    onClick = onClick,
    modifier = modifier.springBounce(interactionSource, pressedScale = 0.972f, enabled = enabled),
    enabled = enabled,
    shape = shape,
    colors = colors,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content,
  )
}

@Composable
fun SpringTextButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
  content: @Composable RowScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  TextButton(
    onClick = onClick,
    modifier = modifier.springBounce(interactionSource, pressedScale = 0.968f, enabled = enabled),
    enabled = enabled,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content,
  )
}

@Composable
fun SpringIconButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  IconButton(
    onClick = onClick,
    modifier = modifier.springBounce(interactionSource, pressedScale = 0.93f, enabled = enabled),
    enabled = enabled,
    interactionSource = interactionSource,
    content = content,
  )
}

@Composable
fun SpringFilterChip(
  selected: Boolean,
  onClick: () -> Unit,
  label: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: @Composable (() -> Unit)? = null,
) {
  val neu = LocalNeuColors.current
  val interactionSource = remember { MutableInteractionSource() }
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = label,
    leadingIcon = leadingIcon,
    interactionSource = interactionSource,
    modifier = modifier.springBounce(interactionSource, pressedScale = 0.972f),
    colors =
      FilterChipDefaults.filterChipColors(
        containerColor = neu.card,
        labelColor = neu.primaryText,
        iconColor = neu.secondaryText,
        selectedContainerColor = neu.accent.copy(alpha = if (neu.isDark) 0.28f else 0.16f),
        selectedLabelColor = if (neu.isDark) Color.White else neu.accent,
        selectedLeadingIconColor = if (neu.isDark) Color.White else neu.accent,
      ),
    border =
      FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = selected,
        borderColor = neu.border,
        selectedBorderColor = neu.accent.copy(alpha = 0.7f),
      ),
  )
}
