package com.fireslav.whitelistchecker.ui.viewmodel

import com.fireslav.whitelistchecker.model.CheckReport
import com.fireslav.whitelistchecker.model.HostCheckResult
import com.fireslav.whitelistchecker.model.HostGroup
import com.fireslav.whitelistchecker.model.NetworkStatusInfo
import com.fireslav.whitelistchecker.model.NetworkType
import com.fireslav.whitelistchecker.model.Verdict

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
