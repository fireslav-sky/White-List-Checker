package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HostCheckResult
import com.example.model.HostGroup

@Composable
fun SettingsScreen(
    hostResults: List<HostCheckResult>,
    onAddHost: (String, HostGroup) -> Unit,
    onRemoveHost: (String, HostGroup) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var hostToDelete by remember { mutableStateOf<Pair<String, HostGroup>?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Header with Material You style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Настройки хостов",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Управление списками проверяемых сервисов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalIconButton(
                    onClick = { showResetConfirmDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reset_hosts_button"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colorScheme.surfaceContainerHighest,
                        contentColor = colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Сбросить к стандартному",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick add card with large button
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Свои сайты для теста",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Добавьте любой ресурс для проверки",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_host_banner_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Добавить", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notice card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Приложение ТОЛЬКО ИЗМЕРЯЕТ доступность ресурсов в текущей сети. Функций обхода, VPN, прокси или модификации сетевого трафика в нём НЕТ.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val whitelist = hostResults.filter { it.group == HostGroup.WHITELIST }
                val control = hostResults.filter { it.group == HostGroup.CONTROL }
                val custom = hostResults.filter { it.group == HostGroup.CUSTOM }

                // Custom section FIRST for immediate visibility & management
                item {
                    SectionHeader(
                        title = "Свои хосты (${custom.size})",
                        description = "Пользовательские домены для проверки доступности"
                    )
                }
                if (custom.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Вы ещё не добавили свои хосты",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Нажмите «Добавить хост» ниже, чтобы добавить сайт для проверки",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(custom, key = { "cfg_custom_${it.host}" }) { item ->
                        HostConfigItem(
                            host = item.host,
                            group = HostGroup.CUSTOM,
                            onDelete = { hostToDelete = Pair(item.host, HostGroup.CUSTOM) }
                        )
                    }
                }

                // Whitelist section
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SectionHeader(
                        title = "Ожидаемо в белом списке (${whitelist.size})",
                        description = "Российские сервисы, банки, государственные порталы"
                    )
                }
                items(whitelist, key = { "cfg_white_${it.host}" }) { item ->
                    HostConfigItem(
                        host = item.host,
                        group = HostGroup.WHITELIST,
                        onDelete = { hostToDelete = Pair(item.host, HostGroup.WHITELIST) }
                    )
                }

                // Control section
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SectionHeader(
                        title = "Контрольные (${control.size})",
                        description = "Зарубежные сервисы, обычно недоступные при белых списках"
                    )
                }
                items(control, key = { "cfg_ctrl_${it.host}" }) { item ->
                    HostConfigItem(
                        host = item.host,
                        group = HostGroup.CONTROL,
                        onDelete = { hostToDelete = Pair(item.host, HostGroup.CONTROL) }
                    )
                }
            }
        }

        // Floating Action Button with large prominent plus icon
        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_host_fab"),
            containerColor = colorScheme.primaryContainer,
            contentColor = colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить хост",
                    modifier = Modifier.size(28.dp)
                )
            },
            text = {
                Text(
                    text = "Добавить сайт",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        )
    }

    // Confirm deletion dialog to prevent accidental clicks and provide clear feedback
    if (hostToDelete != null) {
        val (delHost, delGroup) = hostToDelete!!
        AlertDialog(
            onDismissRequest = { hostToDelete = null },
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Удалить $delHost?",
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Хост «$delHost» будет удалён из списка проверки (${delGroup.displayName}).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveHost(delHost, delGroup)
                        hostToDelete = null
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error,
                        contentColor = colorScheme.onError
                    )
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Удалить")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { hostToDelete = null },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // Add Host Dialog
    if (showAddDialog) {
        AddHostDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { host, group ->
                onAddHost(host, group)
                showAddDialog = false
            }
        )
    }

    // Reset Defaults Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Сбросить списки?", color = colorScheme.onSurface) },
            text = {
                Text(
                    text = "Списки хостов вернутся к исходным (12 белых, 5 контрольных, свои хосты будут удалены).",
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDefaults()
                        showResetConfirmDialog = false
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    )
                ) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetConfirmDialog = false },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, description: String) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HostConfigItem(
    host: String,
    group: HostGroup,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
                if (group == HostGroup.CUSTOM) {
                    Text(
                        text = "Пользовательский",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary
                    )
                }
            }

            // Material 3 minimum 48dp touch target
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("delete_host_$host")
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colorScheme.errorContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить $host",
                        tint = colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddHostDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, HostGroup) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var hostInput by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(HostGroup.CUSTOM) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Добавить хост", color = colorScheme.onSurface) },
        text = {
            Column {
                OutlinedTextField(
                    value = hostInput,
                    onValueChange = {
                        hostInput = it
                        errorText = null
                    },
                    label = { Text("Домен или адрес (например, habr.com)") },
                    singleLine = true,
                    isError = errorText != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("host_input_field"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface,
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline
                    )
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Категория:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurfaceVariant
                )

                HostGroup.values().forEach { group ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            colors = RadioButtonDefaults.colors(selectedColor = colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = group.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = hostInput.trim().lowercase()
                        .removePrefix("https://")
                        .removePrefix("http://")
                        .split("/")[0]
                    if (clean.isEmpty() || !clean.contains(".")) {
                        errorText = "Введите корректное доменное имя (например, mysite.ru)"
                    } else {
                        onConfirm(clean, selectedGroup)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Отмена")
            }
        }
    )
}
