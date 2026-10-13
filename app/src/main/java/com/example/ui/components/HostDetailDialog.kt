package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.model.CheckStep
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
fun HostDetailDialog(
    hostResult: HostCheckResult,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val (badgeBg, badgeText) = when (hostResult.status) {
        HostStatus.AVAILABLE -> Pair(M3GreenContainer, M3GreenText)
        HostStatus.SLOW, HostStatus.TIMEOUT_ERROR -> Pair(M3YellowContainer, M3YellowText)
        HostStatus.NETWORK_ERROR, HostStatus.UNAVAILABLE -> Pair(M3RedContainer, M3RedText)
        HostStatus.CHECKING -> Pair(colorScheme.primaryContainer, colorScheme.onPrimaryContainer)
        HostStatus.IDLE -> Pair(colorScheme.surfaceContainerHighest, colorScheme.onSurfaceVariant)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("host_detail_dialog"),
        containerColor = colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = hostResult.host,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeBg)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        val statusText = when (hostResult.status) {
                            HostStatus.AVAILABLE -> "Доступен (${hostResult.totalLatencyMs} мс)"
                            HostStatus.SLOW -> "Медленно (${hostResult.totalLatencyMs} мс)"
                            HostStatus.TIMEOUT_ERROR -> "Ошибка таймаута"
                            HostStatus.NETWORK_ERROR -> "Ошибка сети"
                            HostStatus.UNAVAILABLE -> "Недоступен"
                            HostStatus.CHECKING -> "Проверка..."
                            HostStatus.IDLE -> "Ожидает"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hostResult.group.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Пошаговая диагностика соединения:",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val allSteps = listOf(
                    CheckStep.DNS,
                    CheckStep.TCP,
                    CheckStep.TLS,
                    CheckStep.HTTP
                )

                allSteps.forEach { step ->
                    val stepResult = hostResult.stepResults.find { it.step == step }
                    StepDetailRow(
                        step = step,
                        isExecuted = stepResult != null,
                        isSuccess = stepResult?.isSuccess ?: false,
                        latencyMs = stepResult?.latencyMs ?: 0L,
                        details = stepResult?.details ?: "Шаг не выполнялся (предыдущий шаг завершился сбоем)"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (hostResult.errorMessage != null && hostResult.status != HostStatus.AVAILABLE && hostResult.status != HostStatus.SLOW) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(badgeBg.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Причина: ${hostResult.errorMessage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = badgeText,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun StepDetailRow(
    step: CheckStep,
    isExecuted: Boolean,
    isSuccess: Boolean,
    latencyMs: Long,
    details: String
) {
    val colorScheme = MaterialTheme.colorScheme
    val (iconBg, iconTint) = when {
        !isExecuted -> Pair(colorScheme.surfaceContainerHighest, colorScheme.onSurfaceVariant)
        isSuccess -> Pair(M3GreenContainer, M3GreenText)
        else -> Pair(M3RedContainer, M3RedText)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.surfaceContainer)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        !isExecuted -> Icons.Default.HourglassEmpty
                        isSuccess -> Icons.Default.Check
                        else -> Icons.Default.Close
                    },
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = step.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    if (isExecuted) {
                        Text(
                            text = "${latencyMs} мс",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isSuccess) colorScheme.onSurfaceVariant else colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isExecuted && !isSuccess) colorScheme.error else colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
