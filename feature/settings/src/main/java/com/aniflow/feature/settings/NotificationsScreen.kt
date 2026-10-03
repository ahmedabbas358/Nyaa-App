package com.aniflow.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aniflow.core.ui.components.AniAppBar
import com.aniflow.core.ui.components.AniEmptyState
import com.aniflow.core.ui.components.AniStatusBadge
import com.aniflow.core.ui.theme.AppSemanticColors
import com.aniflow.core.ui.theme.AppShapes
import com.aniflow.core.ui.theme.AppSpacing
import com.aniflow.core.ui.theme.AppTypography
import com.aniflow.core.ui.theme.DarkBackground
import com.aniflow.core.ui.theme.DarkCardBorder
import com.aniflow.core.ui.theme.DarkSurface
import com.aniflow.core.ui.theme.PrimaryIndigo
import com.aniflow.core.ui.theme.TextMuted
import com.aniflow.core.ui.theme.TextPrimary
import com.aniflow.core.ui.theme.TextSecondary

enum class NotificationCategory(val label: String) {
    All("All"),
    Downloads("Downloads"),
    Anime("Anime"),
    Automation("Automation"),
    System("System")
}

data class AniNotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestampFormatted: String,
    val category: NotificationCategory,
    val isRead: Boolean = false,
    val deepLinkRoute: String? = null
)

/**
 * NotificationsScreen (Section 83).
 * Categorized Notification Center: Downloads, Anime, Automation, System.
 * Actions: Open, Mark Read, Clear.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    onNavigateRoute: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = NotificationCategory.entries.toTypedArray()

    // Real list populated from domain state (empty by default)
    var notifications by remember { mutableStateOf<List<AniNotificationItem>>(emptyList()) }

    val activeCategory = categories[selectedCategoryIndex]
    val filteredNotifications = if (activeCategory == NotificationCategory.All) {
        notifications
    } else {
        notifications.filter { it.category == activeCategory }
    }

    Scaffold(
        topBar = {
            AniAppBar(
                title = "Notifications",
                subtitle = "App alerts & updates",
                onBack = onBack,
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = {
                            notifications = notifications.map { it.copy(isRead = true) }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Mark all as read",
                                tint = PrimaryIndigo
                            )
                        }
                        IconButton(onClick = { notifications = emptyList() }) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear all notifications",
                                tint = TextMuted
                            )
                        }
                    }
                }
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                containerColor = DarkSurface,
                contentColor = TextPrimary,
                edgePadding = AppSpacing.md,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                        color = PrimaryIndigo
                    )
                }
            ) {
                categories.forEachIndexed { index, cat ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = {
                            Text(
                                text = cat.label,
                                style = AppTypography.Subtitle.copy(
                                    fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }

            if (filteredNotifications.isEmpty()) {
                AniEmptyState(
                    title = "No notifications",
                    description = "When downloads finish, automation executes, or new episodes arrive, alerts will appear here.",
                    icon = Icons.Default.NotificationsNone,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredNotifications, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            onClick = {
                                notifications = notifications.map {
                                    if (it.id == item.id) it.copy(isRead = true) else it
                                }
                                item.deepLinkRoute?.let { onNavigateRoute(it) }
                            },
                            onDismiss = {
                                notifications = notifications.filter { it.id != item.id }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: AniNotificationItem,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryIcon = when (item.category) {
        NotificationCategory.Downloads -> Icons.Default.Download
        NotificationCategory.Anime -> Icons.Default.Movie
        NotificationCategory.Automation -> Icons.Default.AutoAwesome
        NotificationCategory.System -> Icons.Default.Settings
        NotificationCategory.All -> Icons.Default.Notifications
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) DarkSurface else DarkSurface.copy(alpha = 0.95f)
        ),
        shape = AppShapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (!item.isRead) PrimaryIndigo.copy(alpha = 0.5f) else DarkCardBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryIndigo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(AppSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.title,
                        style = AppTypography.Subtitle.copy(
                            fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = item.timestampFormatted,
                        style = AppTypography.Caption,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.message,
                    style = AppTypography.BodySmall,
                    color = if (item.isRead) TextMuted else TextSecondary
                )
            }

            if (!item.isRead) {
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PrimaryIndigo)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}
