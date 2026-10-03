package com.aniflow.feature.automation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.aniflow.domain.automation.model.ReviewItem
import com.aniflow.domain.identity.AutomationExecutionId
import com.aniflow.domain.identity.ReviewItemId

/**
 * ReviewQueueScreen (Section 54, 55, 56, 110).
 * Displays decisions requiring user judgment (ambiguous episodes, large batches, conflicts).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewQueueScreen(
    items: List<ReviewItem> = emptyList(),
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val reviewItems = remember(items) {
        mutableStateListOf<ReviewItem>().apply { addAll(items) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automation Review Queue", style = AppTypography.headline, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (reviewItems.isEmpty()) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = AppSemanticColors.Success, modifier = Modifier.padding(16.dp))
                Text("Review Queue is Clean", style = AppTypography.headline, color = TextPrimary)
                Spacer(Modifier.height(AppSpacing.xs))
                Text("No ambiguous releases or unconfirmed batches pending.", style = AppTypography.body, color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                items(reviewItems, key = { it.id.value }) { item ->
                    ReviewItemCard(
                        item = item,
                        onApprove = { reviewItems.remove(item) },
                        onReject = { reviewItems.remove(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewItemCard(
    item: ReviewItem,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(AppShapes.md),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AppSemanticColors.Warning)
                Text(item.issue, style = AppTypography.body.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            }
            Spacer(Modifier.height(AppSpacing.sm))
            Text(item.candidateReleaseTitle, style = AppTypography.body.copy(fontSize = 13.sp), color = PrimaryIndigo)
            Spacer(Modifier.height(AppSpacing.xs))
            Text("Reason: ${item.reason}", style = AppTypography.caption, color = TextSecondary)
            Spacer(Modifier.height(2.dp))
            Text("Recommendation: ${item.recommendedAction}", style = AppTypography.caption, color = TextMuted)

            Spacer(Modifier.height(AppSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppShapes.sm)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Reject")
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(AppShapes.sm)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Approve")
                }
            }
        }
    }
}
