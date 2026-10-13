package com.example.ui.viewmodel

import com.example.model.CheckReport
import com.example.model.HostCheckResult
import com.example.model.HostGroup
import com.example.model.NetworkStatusInfo
import com.example.model.NetworkType
import com.example.model.Verdict

enum class AppScreen(val title: String) {
    CHECK("Проверка"),
    HISTORY("История"),
    SETTINGS("Настройки")
}

data class MainUiState(
    val currentScreen: AppScreen = AppScreen.CHECK,
    val isChecking: Boolean = false,
    val progress: Float = 0f,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val networkInfo: NetworkStatusInfo = NetworkStatusInfo(
        networkType = NetworkType.NO_NETWORK,
        isVpnActive = false,
        networkDescription = "Инициализация сети..."
    ),
    val verdict: Verdict? = null,
    val hostResults: List<HostCheckResult> = emptyList(),
    val selectedHostForDetails: HostCheckResult? = null,
    val historyReports: List<CheckReport> = emptyList(),
    val selectedHistoryReport: CheckReport? = null,
    val filterGroup: HostGroup? = null
)
