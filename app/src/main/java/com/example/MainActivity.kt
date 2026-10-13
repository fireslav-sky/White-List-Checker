package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CheckReport
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MainCheckScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.WhiteListCheckerTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WhiteListCheckerTheme(darkTheme = true, dynamicColor = true) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // BackHandler for secondary screens
                if (uiState.currentScreen != AppScreen.CHECK) {
                    BackHandler {
                        viewModel.setScreen(AppScreen.CHECK)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        BottomNavigationBar(
                            currentScreen = uiState.currentScreen,
                            onSelectScreen = { viewModel.setScreen(it) }
                        )
                    }
                ) { innerPadding ->
                    when (uiState.currentScreen) {
                        AppScreen.CHECK -> {
                            MainCheckScreen(
                                uiState = uiState,
                                onStartCheck = { viewModel.startCheck() },
                                onStopCheck = { viewModel.stopCheck() },
                                onRefreshNetwork = { viewModel.refreshNetworkStatus() },
                                onFilterChange = { viewModel.setFilterGroup(it) },
                                onSelectHost = { viewModel.selectHostForDetails(it) },
                                onShareReport = { shareReportText(viewModel.generateShareReportText()) },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppScreen.HISTORY -> {
                            HistoryScreen(
                                reports = uiState.historyReports,
                                onClearHistory = { viewModel.clearHistory() },
                                onShareReport = { report -> shareReportText(viewModel.generateShareReportText(report)) },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                hostResults = uiState.hostResults,
                                onAddHost = { host, group -> viewModel.addHost(host, group) },
                                onRemoveHost = { host, group -> viewModel.removeHost(host, group) },
                                onResetDefaults = { viewModel.resetToDefaults() },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun shareReportText(text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Поделиться отчётом проверки")
        startActivity(shareIntent)
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    NavigationBar(
        containerColor = colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == AppScreen.CHECK,
            onClick = { onSelectScreen(AppScreen.CHECK) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.CHECK) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = AppScreen.CHECK.title
                )
            },
            label = { Text(AppScreen.CHECK.title) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = colorScheme.secondaryContainer,
                selectedIconColor = colorScheme.onSecondaryContainer,
                selectedTextColor = colorScheme.onSurface,
                unselectedIconColor = colorScheme.onSurfaceVariant,
                unselectedTextColor = colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("tab_check")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.HISTORY,
            onClick = { onSelectScreen(AppScreen.HISTORY) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                    contentDescription = AppScreen.HISTORY.title
                )
            },
            label = { Text(AppScreen.HISTORY.title) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = colorScheme.secondaryContainer,
                selectedIconColor = colorScheme.onSecondaryContainer,
                selectedTextColor = colorScheme.onSurface,
                unselectedIconColor = colorScheme.onSurfaceVariant,
                unselectedTextColor = colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("tab_history")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.SETTINGS,
            onClick = { onSelectScreen(AppScreen.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = AppScreen.SETTINGS.title
                )
            },
            label = { Text(AppScreen.SETTINGS.title) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = colorScheme.secondaryContainer,
                selectedIconColor = colorScheme.onSecondaryContainer,
                selectedTextColor = colorScheme.onSurface,
                unselectedIconColor = colorScheme.onSurfaceVariant,
                unselectedTextColor = colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("tab_settings")
        )
    }
}
