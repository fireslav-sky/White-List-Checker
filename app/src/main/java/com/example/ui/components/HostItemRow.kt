package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HostCheckResult
import com.example.model.HostStatus
import com.example.ui.theme.M3Green
import com.example.ui.theme.M3GreenContainer
import com.example.ui.theme.M3GreenText
import com.example.ui.theme.M3Red
import com.example.ui.theme.M3RedContainer
import com.example.ui.theme.M3RedText
import com.example.ui.theme.M3Yellow
import com.example.ui.theme.M3YellowContainer
import com.example.ui.theme.M3YellowText

@Composable
fun HostItemRow(
    item: HostCheckResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val (statusDotColor, statusBgColor, statusTextColor) = when (item.status) {
        HostStatus.AVAILABLE -> Triple(M3Green, M3GreenContainer, M3GreenText)
        HostStatus.SLOW -> Triple(M3Yellow, M3YellowContainer, M3YellowText)
        HostStatus.TIMEOUT_ERROR -> Triple(M3Yellow, M3YellowContainer, M3YellowText)
        HostStatus.NETWORK_ERROR, HostStatus.UNAVAILABLE -> Triple(M3Red, M3RedContainer, M3RedText)
        HostStatus.CHECKING -> Triple(colorScheme.primary, colorScheme.primaryContainer.copy(alpha = 0.5f), colorScheme.primary)
        HostStatus.IDLE -> Triple(colorScheme.outlineVariant, colorScheme.surfaceContainerHighest, colorScheme.onSurfaceVariant)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("host_item_${item.host}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Host info & status dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (item.status == HostStatus.CHECKING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.5.dp,
                        color = colorScheme.primary
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = item.host,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    if (item.errorMessage != null && (item.status == HostStatus.UNAVAILABLE || item.status == HostStatus.NETWORK_ERROR || item.status == HostStatus.TIMEOUT_ERROR)) {
                        Text(
                            text = item.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusTextColor,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusBgColor)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                val labelText = when (item.status) {
                    HostStatus.AVAILABLE -> "${item.totalLatencyMs} мс"
                    HostStatus.SLOW -> "${item.totalLatencyMs} мс"
                    HostStatus.TIMEOUT_ERROR -> "Таймаут"
                    HostStatus.NETWORK_ERROR -> item.failedStep?.displayName ?: "Ошибка сети"
                    HostStatus.UNAVAILABLE -> item.failedStep?.displayName ?: "Сбой"
                    HostStatus.CHECKING -> "Тест..."
                    HostStatus.IDLE -> "Ожидает"
                }

                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = statusTextColor
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Подробнее",
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
