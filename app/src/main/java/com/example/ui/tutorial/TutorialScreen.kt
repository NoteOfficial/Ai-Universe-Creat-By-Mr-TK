package com.example.ui.tutorial

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.localization.UiStrings
import com.example.ui.theme.LocalNeuColors
import com.example.ui.theme.NeuCard
import kotlinx.coroutines.launch

@Composable
fun TutorialScreen(
  strings: UiStrings,
  reducedMotion: Boolean,
  onFinish: () -> Unit,
) {
  val neu = LocalNeuColors.current
  val pages = strings.tutorialPages
  val pagerState = rememberPagerState(pageCount = { pages.size })
  val scope = rememberCoroutineScope()

  BackHandler(enabled = pagerState.currentPage > 0) {
    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(neu.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 20.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "${pagerState.currentPage + 1} / ${pages.size}",
        style = MaterialTheme.typography.labelLarge,
        color = neu.secondaryText,
      )
      TextButton(
        onClick = onFinish,
        modifier = Modifier.testTag("tutorial_skip_button"),
      ) {
        Text(text = strings.skip, color = neu.accent)
      }
    }

    HorizontalPager(
      state = pagerState,
      modifier = Modifier.weight(1f).fillMaxWidth(),
    ) { page ->
      val item = pages[page]
      Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        NeuCard(
          modifier = Modifier.fillMaxWidth().heightIn(min = 210.dp, max = 280.dp),
          cornerRadius = 28.dp,
        ) {
          TutorialVectorIllustration(pageIndex = page, reducedMotion = reducedMotion)
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
          text = item.title,
          style = MaterialTheme.typography.headlineMedium,
          color = neu.primaryText,
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = item.description,
          style = MaterialTheme.typography.bodyLarge,
          color = neu.secondaryText,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 8.dp),
        )
      }
    }

    // Page Indicators
    Row(
      modifier = Modifier.padding(vertical = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      repeat(pages.size) { index ->
        val active = pagerState.currentPage == index
        val color by
          animateColorAsState(
            targetValue = if (active) neu.accent else neu.border,
            label = "dot_color",
          )
        Box(
          modifier =
            Modifier.height(8.dp)
              .width(if (active) 24.dp else 8.dp)
              .clip(CircleShape)
              .background(color)
        )
      }
    }

    // Controls: Back & Next / Continue / Get Started
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (pagerState.currentPage > 0) {
        OutlinedButton(
          onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
          modifier = Modifier.weight(1f).height(52.dp).testTag("tutorial_back_button"),
          shape = RoundedCornerShape(16.dp),
        ) {
          Text(strings.back)
        }
      } else {
        Spacer(modifier = Modifier.weight(1f))
      }

      val isLast = pagerState.currentPage == pages.lastIndex
      Button(
        onClick = {
          if (isLast) {
            onFinish()
          } else {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
          }
        },
        modifier = Modifier.weight(1.4f).height(52.dp).testTag("tutorial_next_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
      ) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.horizontalGradient(listOf(neu.accent, neu.accentSecondary)),
                RoundedCornerShape(16.dp),
              ),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text =
              when {
                isLast -> strings.getStarted
                pagerState.currentPage == 0 -> strings.continueLabel
                else -> strings.next
              },
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}
