package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HostCheckResult
import com.example.model.HostGroup
import com.example.model.HostStatus
import com.example.model.Verdict
import com.example.model.VerdictType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerdictCard(
    verdict: Verdict,
    results: List<HostCheckResult>,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val (badgeContainer, badgeContent, icon) = when (verdict.type) {
        VerdictType.WHITELIST_ACTIVE -> Triple(colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer, Icons.Default.Shield)
        VerdictType.NO_RESTRICTIONS -> Triple(colorScheme.primaryContainer, colorScheme.onPrimaryContainer, Icons.Default.CheckCircle)
        VerdictType.NO_CONNECTION -> Triple(colorScheme.errorContainer, colorScheme.onErrorContainer, Icons.Default.Error)
        VerdictType.MIXED -> Triple(colorScheme.secondaryContainer, colorScheme.onSecondaryContainer, Icons.Default.Warning)
        VerdictType.PENDING -> Triple(colorScheme.surfaceContainerHighest, colorScheme.onSurfaceVariant, Icons.Default.Help)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("verdict_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with heuristic tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeContainer)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Эвристический вердикт",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeContent
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(badgeContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeContent,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = verdict.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        lineHeight = 26.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = verdict.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics badges
            val whiteList = results.filter { it.group == HostGroup.WHITELIST }
            val controlList = results.filter { it.group == HostGroup.CONTROL }
            val customList = results.filter { it.group == HostGroup.CUSTOM }

            val whiteOk = whiteList.count { it.status == HostStatus.AVAILABLE || it.status == HostStatus.SLOW }
            val controlOk = controlList.count { it.status == HostStatus.AVAILABLE || it.status == HostStatus.SLOW }
            val customOk = customList.count { it.status == HostStatus.AVAILABLE || it.status == HostStatus.SLOW }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (whiteList.isNotEmpty()) {
                    M3StatChip(
                        label = "Белый список",
                        count = "$whiteOk/${whiteList.size}",
                        isFavorable = whiteOk > 0
                    )
                }
                if (controlList.isNotEmpty()) {
                    M3StatChip(
                        label = "Контрольные",
                        count = "$controlOk/${controlList.size}",
                        isFavorable = controlOk > 0
                    )
                }
                if (customList.isNotEmpty()) {
                    M3StatChip(
                        label = "Свои хосты",
                        count = "$customOk/${customList.size}",
                        isFavorable = customOk > 0
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Share Report Button in Material You style
            FilledTonalButton(
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("share_report_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colorScheme.secondaryContainer,
                    contentColor = colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Поделиться отчётом",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun M3StatChip(
    label: String,
    count: String,
    isFavorable: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                text = count,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isFavorable) colorScheme.primary else colorScheme.error
            )
        }
    }
}
