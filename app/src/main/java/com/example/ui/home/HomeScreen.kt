package com.example.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ActivityLogEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.WorkspaceEntity
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.localization.UiStrings
import com.example.ui.localization.categoryName
import com.example.ui.localization.pricingName
import com.example.ui.localization.sortName
import com.example.ui.theme.*
import com.example.ui.viewmodel.SortOption

@Composable
fun HomeScreen(
  strings: UiStrings,
  platforms: List<AiPlatform>,
  workspaces: List<WorkspaceEntity>,
  projects: List<ProjectEntity>,
  logs: List<ActivityLogEntity>,
  networkStatus: NetworkStatus,
  searchQuery: String,
  selectedCategory: AiCategory?,
  selectedPricing: PricingType?,
  sortOption: SortOption,
  recentSearches: List<String>,
  onSearchChange: (String) -> Unit,
  onCommitSearch: (String) -> Unit,
  onSelectCategory: (AiCategory?) -> Unit,
  onSelectPricing: (PricingType?) -> Unit,
  onSelectSort: (SortOption) -> Unit,
  onOpenAiDetails: (AiPlatform) -> Unit,
  onQuickStartAi: (AiPlatform) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onTogglePin: (String) -> Unit,
  onReorderAi: (String, Int) -> Unit,
  onRestoreWorkspace: (String) -> Unit,
  onCloseWorkspace: (String) -> Unit,
  onOpenApiChat: () -> Unit,
  onOpenFileManager: () -> Unit,
  onOpenDownloads: () -> Unit,
  onOpenProjectManager: () -> Unit,
  onOpenWorkspaceManager: () -> Unit,
  onOpenAddCustomAi: () -> Unit,
  onOpenComparison: () -> Unit,
  onOpenClipboard: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenSettings: () -> Unit,
  listState: LazyListState = rememberLazyListState(),
  modifier: Modifier = Modifier,
) {
  val neu = LocalNeuColors.current
  var showSortMenu by remember { mutableStateOf(false) }
  var reorderMode by remember { mutableStateOf(false) }

  val filteredPlatforms =
    remember(platforms, searchQuery, selectedCategory, selectedPricing, sortOption) {
      val q = searchQuery.trim().lowercase()
      val base =
        platforms.filter { ai ->
          val matchQuery =
            q.isEmpty() ||
              ai.name.lowercase().contains(q) ||
              ai.shortDescription.lowercase().contains(q) ||
              ai.category.label.lowercase().contains(q)
          val matchCat = selectedCategory == null || ai.category == selectedCategory
          val matchPrice = selectedPricing == null || ai.pricingType == selectedPricing
          matchQuery && matchCat && matchPrice
        }
      when (sortOption) {
        SortOption.DEFAULT -> base
        SortOption.NAME_ASC -> base.sortedBy { it.name.lowercase() }
        SortOption.FAVORITES_FIRST -> base.sortedByDescending { it.isFavorite }
        SortOption.CATEGORY -> base.sortedBy { it.category.label }
      }
    }

  val pinnedPlatforms = remember(platforms) { platforms.filter { it.isPinned } }
  val unreadAlerts = remember(logs) { logs.count { !it.isRead } }

  LazyColumn(
    state = listState,
    modifier = modifier.fillMaxSize().background(neu.background),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Top Header
    item(key = "header", contentType = "header") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_launcher_fg_art),
            contentDescription = "AI UNIVERSE Logo",
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop,
          )
          Column {
            Text(
              text = strings.appName,
              style = MaterialTheme.typography.titleLarge,
              color = neu.primaryText,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = strings.tagline,
              style = MaterialTheme.typography.bodySmall,
              color = neu.secondaryText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          NetworkStatusPill(status = networkStatus)
          BadgedBox(
            badge = {
              if (unreadAlerts > 0) {
                Badge(containerColor = neu.accent) { Text("$unreadAlerts") }
              }
            }
          ) {
            NeuIconButton(
              onClick = onOpenNotifications,
              modifier = Modifier.testTag("notifications_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = strings.notificationsCenterTitle,
                tint = neu.primaryText,
              )
            }
          }
        }
      }
    }

    // Colorful Theme-Reactive Hero Banner Card
    item(key = "hero", contentType = "hero") {
      NeuCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(0.dp),
      ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 136.dp)) {
          Image(
            painter = painterResource(id = R.drawable.img_universe_banner),
            contentDescription = "AI Universe Cosmic Banner",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
          )
          Box(
            modifier =
              Modifier.matchParentSize()
                .background(
                  Brush.horizontalGradient(
                    listOf(
                      neu.accent.copy(alpha = 0.88f),
                      Color(0xB814182D),
                      neu.accentSecondary.copy(alpha = 0.60f),
                    )
                  )
                )
          )
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = String.format(strings.verifiedPlatformsFmt, platforms.size),
              style = MaterialTheme.typography.headlineSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = String.format(strings.heroSubtitleFmt, workspaces.size, projects.size),
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.90f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              SpringButton(
                onClick = onOpenApiChat,
                colors = ButtonDefaults.buttonColors(containerColor = neu.accentSecondary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text(strings.apiAiChat, style = MaterialTheme.typography.labelLarge, color = Color.White)
              }
              SpringOutlinedButton(
                onClick = onOpenWorkspaceManager,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              ) {
                Text("${strings.workspaceManager} (${workspaces.size})")
              }
            }
          }
        }
      }
    }

    // Global Search Bar
    item(key = "search", contentType = "search") {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NeuCard(
          modifier = Modifier.fillMaxWidth(),
          cornerRadius = 18.dp,
          elevated = false,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = null,
              tint = neu.accent,
            )
            TextField(
              value = searchQuery,
              onValueChange = onSearchChange,
              placeholder = {
                Text(
                  strings.searchHint,
                  style = MaterialTheme.typography.bodyMedium,
                  color = neu.secondaryText,
                )
              },
              singleLine = true,
              colors =
                TextFieldDefaults.colors(
                  focusedContainerColor = Color.Transparent,
                  unfocusedContainerColor = Color.Transparent,
                  focusedIndicatorColor = Color.Transparent,
                  unfocusedIndicatorColor = Color.Transparent,
                ),
              modifier = Modifier.weight(1f).testTag("home_search_input"),
            )
            if (searchQuery.isNotBlank()) {
              IconButton(
                onClick = {
                  onCommitSearch(searchQuery)
                  onSearchChange("")
                }
              ) {
                Icon(Icons.Rounded.Close, contentDescription = strings.clearSearch)
              }
            }
          }
        }
        if (recentSearches.isNotEmpty() && searchQuery.isBlank()) {
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            recentSearches.forEach { term ->
              AssistChip(
                onClick = { onSearchChange(term) },
                label = { Text(term, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                  Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(14.dp))
                },
              )
            }
          }
        }
      }
    }

    // Colorful Quick Actions Strip
    item(key = "quick_actions", contentType = "quick_actions") {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = strings.quickActions,
          style = MaterialTheme.typography.titleMedium,
          color = neu.primaryText,
          fontWeight = FontWeight.Bold,
        )
        val actions =
          remember(
            strings,
            onOpenApiChat,
            onOpenFileManager,
            onOpenDownloads,
            onOpenProjectManager,
            onOpenAddCustomAi,
            onOpenComparison,
            onOpenClipboard,
            onOpenSettings,
          ) {
            listOf(
              Triple(strings.apiAiChat, Icons.Rounded.Forum, onOpenApiChat),
              Triple(strings.fileManager, Icons.Rounded.FolderOpen, onOpenFileManager),
              Triple(strings.downloads, Icons.Rounded.Download, onOpenDownloads),
              Triple(strings.projectManager, Icons.Rounded.Workspaces, onOpenProjectManager),
              Triple(strings.addCustomAi, Icons.Rounded.AddCircleOutline, onOpenAddCustomAi),
              Triple(strings.compareAi, Icons.Rounded.CompareArrows, onOpenComparison),
              Triple(strings.clipboard, Icons.Rounded.ContentPaste, onOpenClipboard),
              Triple(strings.settings, Icons.Rounded.Settings, onOpenSettings),
            )
          }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          itemsIndexed(actions, key = { idx, item -> "qa_$idx" }) { idx, (label, icon, onClick) ->
            val tileTint = VibrantPaletteAccents[idx % VibrantPaletteAccents.size]
            QuickActionTile(label = label, icon = icon, accentColor = tileTint, onClick = onClick)
          }
        }
      }
    }

    // Active & Minimized Workspaces
    if (workspaces.isNotEmpty()) {
      item(key = "workspaces", contentType = "workspaces") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = strings.recentWorkspaces,
              style = MaterialTheme.typography.titleMedium,
              color = neu.primaryText,
              fontWeight = FontWeight.Bold,
            )
            SpringTextButton(onClick = onOpenWorkspaceManager) { Text(strings.manageAll) }
          }
          LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(workspaces, key = { it.id }) { ws ->
              val ai = platforms.find { it.id == ws.aiId }
              NeuCard(
                modifier = Modifier.width(220.dp),
                cornerRadius = 18.dp,
                elevated = false,
                onClick = { onRestoreWorkspace(ws.id) },
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      modifier = Modifier.weight(1f),
                    ) {
                      if (ai != null) {
                        AiBrandBadge(platform = ai, size = 32.dp)
                      }
                      Column {
                        Text(
                          ws.title,
                          style = MaterialTheme.typography.titleSmall,
                          color = neu.primaryText,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          if (ws.isMinimized) strings.minimizedLabel else strings.activeLabel,
                          style = MaterialTheme.typography.labelSmall,
                          color = if (ws.isMinimized) WarningAmber else SuccessGreen,
                        )
                      }
                    }
                    IconButton(
                      onClick = { onCloseWorkspace(ws.id) },
                      modifier = Modifier.size(32.dp),
                    ) {
                      Icon(
                        Icons.Rounded.Close,
                        contentDescription = strings.closeWorkspace,
                        modifier = Modifier.size(16.dp),
                        tint = neu.secondaryText,
                      )
                    }
                  }
                  Text(
                    text = ws.currentUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = neu.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }
    }

    // Pinned AI Section
    if (pinnedPlatforms.isNotEmpty()) {
      item(key = "pinned", contentType = "pinned") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = strings.pinnedAi,
            style = MaterialTheme.typography.titleMedium,
            color = neu.primaryText,
            fontWeight = FontWeight.Bold,
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(pinnedPlatforms, key = { "pin_${it.id}" }) { ai ->
              NeuCard(
                modifier = Modifier.width(170.dp),
                cornerRadius = 18.dp,
                elevated = false,
                onClick = { onQuickStartAi(ai) },
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    AiBrandBadge(platform = ai, size = 38.dp)
                    IconButton(
                      onClick = { onTogglePin(ai.id) },
                      modifier = Modifier.size(32.dp),
                    ) {
                      Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = "${strings.pin} ${ai.name}",
                        tint = neu.accent,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                  Text(
                    ai.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = neu.primaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    strings.tapToLaunch,
                    style = MaterialTheme.typography.bodySmall,
                    color = neu.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }
    }

    // Pricing Filter & Category Chips & Sort Bar
    item(key = "filters", contentType = "filters") {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${strings.categories} (${filteredPlatforms.size})",
            style = MaterialTheme.typography.titleMedium,
            color = neu.primaryText,
            fontWeight = FontWeight.Bold,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SpringFilterChip(
              selected = reorderMode,
              onClick = { reorderMode = !reorderMode },
              label = { Text(strings.reorder) },
              leadingIcon = {
                Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
              },
            )
            Box {
              SpringFilterChip(
                selected = sortOption != SortOption.DEFAULT,
                onClick = { showSortMenu = true },
                label = { Text(strings.sort) },
                leadingIcon = {
                  Icon(Icons.Rounded.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                },
              )
              DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                SortOption.entries.forEach { option ->
                  DropdownMenuItem(
                    text = { Text(strings.sortName(option)) },
                    onClick = {
                      onSelectSort(option)
                      showSortMenu = false
                    },
                    trailingIcon = {
                      if (sortOption == option) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                      }
                    },
                  )
                }
              }
            }
          }
        }

        // Pricing Tabs
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SpringFilterChip(
            selected = selectedPricing == null,
            onClick = { onSelectPricing(null) },
            label = { Text(strings.allPlans) },
          )
          PricingType.entries.forEach { pricing ->
            SpringFilterChip(
              selected = selectedPricing == pricing,
              onClick = { onSelectPricing(if (selectedPricing == pricing) null else pricing) },
              label = { Text(strings.pricingName(pricing)) },
            )
          }
        }

        // 9 Categories
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SpringFilterChip(
            selected = selectedCategory == null,
            onClick = { onSelectCategory(null) },
            label = { Text(strings.allCategories) },
          )
          AiCategory.entries.forEach { cat ->
            SpringFilterChip(
              selected = selectedCategory == cat,
              onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) },
              label = { Text(strings.categoryName(cat)) },
            )
          }
        }
      }
    }

    // AI Directory List — flat elevated=false cards for ultra-smooth 60/120fps scrolling
    if (filteredPlatforms.isEmpty()) {
      item(key = "empty_state", contentType = "empty_state") {
        NeuCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.SearchOff, contentDescription = null, tint = neu.secondaryText, modifier = Modifier.size(40.dp))
            Text(strings.noMatchingAi, style = MaterialTheme.typography.titleSmall, color = neu.primaryText)
            SpringTextButton(onClick = {
              onSearchChange("")
              onSelectCategory(null)
              onSelectPricing(null)
            }) {
              Text(strings.resetFilters)
            }
          }
        }
      }
    } else {
      items(filteredPlatforms, key = { it.id }, contentType = { "ai_card" }) { platform ->
        AiDirectoryCard(
          platform = platform,
          strings = strings,
          reorderMode = reorderMode,
          onOpenDetails = { onOpenAiDetails(platform) },
          onQuickLaunch = { onQuickStartAi(platform) },
          onToggleFavorite = { onToggleFavorite(platform.id) },
          onTogglePin = { onTogglePin(platform.id) },
          onMoveUp = { onReorderAi(platform.id, -1) },
          onMoveDown = { onReorderAi(platform.id, 1) },
          modifier = if (reorderMode) Modifier.animateItem() else Modifier,
        )
      }
    }

    // Recent Projects Preview
    if (projects.isNotEmpty()) {
      item(key = "recent_projects", contentType = "projects") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = strings.recentProjects,
              style = MaterialTheme.typography.titleMedium,
              color = neu.primaryText,
              fontWeight = FontWeight.Bold,
            )
            SpringTextButton(onClick = onOpenProjectManager) { Text(strings.viewAll) }
          }
          projects.take(3).forEach { proj ->
            NeuCard(
              modifier = Modifier.fillMaxWidth(),
              elevated = false,
              onClick = onOpenProjectManager,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(proj.name, style = MaterialTheme.typography.titleSmall, color = neu.primaryText)
                  Text(
                    "${proj.aiName} • ${proj.url}",
                    style = MaterialTheme.typography.labelSmall,
                    color = neu.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = neu.secondaryText)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuickActionTile(
  label: String,
  icon: ImageVector,
  accentColor: Color,
  onClick: () -> Unit,
) {
  val neu = LocalNeuColors.current
  NeuCard(
    modifier = Modifier.width(104.dp),
    cornerRadius = 18.dp,
    elevated = false,
    onClick = onClick,
    contentPadding = PaddingValues(12.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier =
          Modifier.size(40.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = neu.primaryText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun AiDirectoryCard(
  platform: AiPlatform,
  strings: UiStrings,
  reorderMode: Boolean,
  onOpenDetails: () -> Unit,
  onQuickLaunch: () -> Unit,
  onToggleFavorite: () -> Unit,
  onTogglePin: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val neu = LocalNeuColors.current
  NeuCard(
    modifier = modifier.fillMaxWidth().testTag("ai_card_${platform.id}"),
    cornerRadius = 20.dp,
    elevated = false,
    onClick = onOpenDetails,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        AiBrandBadge(platform = platform, size = 48.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = platform.name,
              style = MaterialTheme.typography.titleMedium,
              color = neu.primaryText,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (platform.isCustom) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = neu.accent.copy(alpha = 0.15f),
              ) {
                Text(
                  strings.customBadge,
                  style = MaterialTheme.typography.labelSmall,
                  color = neu.accent,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }
          Text(
            text = strings.categoryName(platform.category),
            style = MaterialTheme.typography.labelMedium,
            color = neu.accent,
            fontWeight = FontWeight.Medium,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (reorderMode) {
            IconButton(onClick = onMoveUp) {
              Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move ${platform.name} up")
            }
            IconButton(onClick = onMoveDown) {
              Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move ${platform.name} down")
            }
          } else {
            IconButton(
              onClick = onToggleFavorite,
              modifier = Modifier.testTag("fav_${platform.id}"),
            ) {
              Icon(
                imageVector =
                  if (platform.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "${strings.favorites} ${platform.name}",
                tint = if (platform.isFavorite) ErrorRed else neu.secondaryText,
              )
            }
            IconButton(
              onClick = onTogglePin,
              modifier = Modifier.testTag("pin_${platform.id}"),
            ) {
              Icon(
                imageVector = if (platform.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                contentDescription = "${strings.pin} ${platform.name}",
                tint = if (platform.isPinned) neu.accent else neu.secondaryText,
              )
            }
          }
        }
      }

      Text(
        text = platform.shortDescription,
        style = MaterialTheme.typography.bodyMedium,
        color = neu.secondaryText,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          PricingBadge(pricingType = platform.pricingType)
          CompatibilityBadge(status = platform.compatibility)
        }
        SpringTextButton(
          onClick = onQuickLaunch,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) {
          Text(strings.openLabel, color = neu.accent, fontWeight = FontWeight.SemiBold)
          Spacer(Modifier.width(2.dp))
          Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = neu.accent,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}
