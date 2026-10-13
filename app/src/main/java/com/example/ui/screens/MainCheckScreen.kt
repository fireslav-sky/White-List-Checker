package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HostGroup
import com.example.ui.components.HostDetailDialog
import com.example.ui.components.HostItemRow
import com.example.ui.components.NetworkStatusCard
import com.example.ui.components.VerdictCard
import com.example.ui.viewmodel.MainUiState

@Composable
fun MainCheckScreen(
    uiState: MainUiState,
    onStartCheck: () -> Unit,
    onStopCheck: () -> Unit,
    onRefreshNetwork: () -> Unit,
    onFilterChange: (HostGroup?) -> Unit,
    onSelectHost: (com.example.model.HostCheckResult?) -> Unit,
    onShareReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Material 3 Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp)
            ) {
                Text(
                    text = "White List Checker",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Диагностика доступности сети и белых списков",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Network Status Card
                item {
                    NetworkStatusCard(
                        networkInfo = uiState.networkInfo,
                        onRefresh = onRefreshNetwork
                    )
                }

                // Primary Action Button (Material You Pill Shape)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (uiState.isChecking) {
                            Button(
                                onClick = onStopCheck,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("check_button"),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.error,
                                    contentColor = colorScheme.onError
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Остановить проверку",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = onStartCheck,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("check_button"),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary,
                                    contentColor = colorScheme.onPrimary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Начать проверку",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Progress bar with Material 3 rounded caps
                        AnimatedVisibility(visible = uiState.isChecking) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { uiState.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = colorScheme.primary,
                                    trackColor = colorScheme.surfaceContainerHighest,
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Проверено: ${uiState.completedCount} из ${uiState.totalCount} хостов",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(uiState.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Verdict Card
                if (uiState.verdict != null) {
                    item {
                        VerdictCard(
                            verdict = uiState.verdict,
                            results = uiState.hostResults,
                            onShare = onShareReport
                        )
                    }
                }

                // Horizontally Scrollable Material You Filter Chips
                item {
                    val filterScrollState = rememberScrollState()
                    val whiteCount = uiState.hostResults.count { it.group == HostGroup.WHITELIST }
                    val controlCount = uiState.hostResults.count { it.group == HostGroup.CONTROL }
                    val customCount = uiState.hostResults.count { it.group == HostGroup.CUSTOM }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(filterScrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = uiState.filterGroup == null,
                            onClick = { onFilterChange(null) },
                            label = {
                                Text(
                                    text = "Все (${uiState.hostResults.size})",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.secondaryContainer,
                                selectedLabelColor = colorScheme.onSecondaryContainer,
                                containerColor = colorScheme.surfaceContainer,
                                labelColor = colorScheme.onSurfaceVariant
                            )
                        )

                        FilterChip(
                            selected = uiState.filterGroup == HostGroup.WHITELIST,
                            onClick = { onFilterChange(HostGroup.WHITELIST) },
                            label = {
                                Text(
                                    text = "Белые ($whiteCount)",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.secondaryContainer,
                                selectedLabelColor = colorScheme.onSecondaryContainer,
                                containerColor = colorScheme.surfaceContainer,
                                labelColor = colorScheme.onSurfaceVariant
                            )
                        )

                        FilterChip(
                            selected = uiState.filterGroup == HostGroup.CONTROL,
                            onClick = { onFilterChange(HostGroup.CONTROL) },
                            label = {
                                Text(
                                    text = "Контрольные ($controlCount)",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.secondaryContainer,
                                selectedLabelColor = colorScheme.onSecondaryContainer,
                                containerColor = colorScheme.surfaceContainer,
                                labelColor = colorScheme.onSurfaceVariant
                            )
                        )

                        FilterChip(
                            selected = uiState.filterGroup == HostGroup.CUSTOM,
                            onClick = { onFilterChange(HostGroup.CUSTOM) },
                            label = {
                                Text(
                                    text = "Свои ($customCount)",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.secondaryContainer,
                                selectedLabelColor = colorScheme.onSecondaryContainer,
                                containerColor = colorScheme.surfaceContainer,
                                labelColor = colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Grouped Host List
                val filteredHosts = if (uiState.filterGroup == null) {
                    uiState.hostResults
                } else {
                    uiState.hostResults.filter { it.group == uiState.filterGroup }
                }

                if (filteredHosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Нет хостов в выбранной категории",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    if (uiState.filterGroup == null) {
                        val whiteListItems = filteredHosts.filter { it.group == HostGroup.WHITELIST }
                        val controlItems = filteredHosts.filter { it.group == HostGroup.CONTROL }
                        val customItems = filteredHosts.filter { it.group == HostGroup.CUSTOM }

                        if (whiteListItems.isNotEmpty()) {
                            item {
                                GroupSectionHeader(
                                    title = HostGroup.WHITELIST.displayName,
                                    subtitle = "Российские сервисы, ожидаемо доступные (${whiteListItems.size})"
                                )
                            }
                            items(whiteListItems, key = { "white_${it.host}" }) { item ->
                                HostItemRow(
                                    item = item,
                                    onClick = { onSelectHost(item) }
                                )
                            }
                        }

                        if (controlItems.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                GroupSectionHeader(
                                    title = HostGroup.CONTROL.displayName,
                                    subtitle = "Внешние ресурсы для контроля блокировок (${controlItems.size})"
                                )
                            }
                            items(controlItems, key = { "control_${it.host}" }) { item ->
                                HostItemRow(
                                    item = item,
                                    onClick = { onSelectHost(item) }
                                )
                            }
                        }

                        if (customItems.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                GroupSectionHeader(
                                    title = HostGroup.CUSTOM.displayName,
                                    subtitle = "Пользовательские ресурсы (${customItems.size})"
                                )
                            }
                            items(customItems, key = { "custom_${it.host}" }) { item ->
                                HostItemRow(
                                    item = item,
                                    onClick = { onSelectHost(item) }
                                )
                            }
                        }
                    } else {
                        items(filteredHosts, key = { "${it.group}_${it.host}" }) { item ->
                            HostItemRow(
                                item = item,
                                onClick = { onSelectHost(item) }
                            )
                        }
                    }
                }
            }
        }

        // Host Details Breakdown Dialog
        if (uiState.selectedHostForDetails != null) {
            HostDetailDialog(
                hostResult = uiState.selectedHostForDetails,
                onDismiss = { onSelectHost(null) }
            )
        }
    }
}

@Composable
private fun GroupSectionHeader(title: String, subtitle: String) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
    }
}
