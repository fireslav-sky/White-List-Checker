package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HostProbeService
import com.example.data.NetworkStatusHelper
import com.example.data.PreferencesRepository
import com.example.model.CheckReport
import com.example.model.HostCheckResult
import com.example.model.HostGroup
import com.example.model.HostItem
import com.example.model.HostStatus
import com.example.model.NetworkType
import com.example.model.Verdict
import com.example.model.VerdictType
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)
    private val networkStatusHelper = NetworkStatusHelper(application)
    private val hostProbeService = HostProbeService()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var currentConfiguredHosts: List<HostItem> = emptyList()
    private var checkJob: Job? = null

    init {
        refreshNetworkStatus()

        viewModelScope.launch {
            preferencesRepository.hostItemsFlow.collect { items ->
                currentConfiguredHosts = items
                if (!_uiState.value.isChecking) {
                    _uiState.update { state ->
                        // Preserve previous test results for hosts that still exist, or create new IDLE entry
                        val existingMap = state.hostResults.associateBy { it.host }
                        val updatedResults = items.map { item ->
                            existingMap[item.host]?.copy(group = item.group)
                                ?: HostCheckResult(host = item.host, group = item.group, status = HostStatus.IDLE)
                        }
                        state.copy(
                            hostResults = updatedResults,
                            totalCount = updatedResults.size
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            preferencesRepository.historyReportsFlow.collect { reports ->
                _uiState.update { it.copy(historyReports = reports) }
            }
        }
    }

    fun setScreen(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setFilterGroup(group: HostGroup?) {
        _uiState.update { it.copy(filterGroup = group) }
    }

    fun selectHostForDetails(result: HostCheckResult?) {
        _uiState.update { it.copy(selectedHostForDetails = result) }
    }

    fun selectHistoryReport(report: CheckReport?) {
        _uiState.update { it.copy(selectedHistoryReport = report) }
    }

    fun refreshNetworkStatus() {
        val netInfo = networkStatusHelper.getCurrentNetworkStatus()
        _uiState.update { it.copy(networkInfo = netInfo) }
    }

    fun startCheck() {
        checkJob?.cancel()
        refreshNetworkStatus()

        val hostsToCheck = if (currentConfiguredHosts.isNotEmpty()) {
            currentConfiguredHosts
        } else {
            PreferencesRepository.DEFAULT_WHITELIST.map { HostItem(it, HostGroup.WHITELIST) } +
            PreferencesRepository.DEFAULT_CONTROL.map { HostItem(it, HostGroup.CONTROL) }
        }

        // Initialize UI with all hosts set to IDLE
        val initialResults = hostsToCheck.map { item ->
            HostCheckResult(host = item.host, group = item.group, status = HostStatus.IDLE)
        }

        _uiState.update { state ->
            state.copy(
                isChecking = true,
                progress = 0f,
                completedCount = 0,
                totalCount = initialResults.size,
                verdict = null,
                hostResults = initialResults
            )
        }

        checkJob = viewModelScope.launch {
            val semaphore = Semaphore(6) // Parallel limit of 6 checks
            var completed = 0
            val total = initialResults.size

            val deferredResults = initialResults.map { initialItem ->
                async {
                    semaphore.withPermit {
                        // Mark as checking
                        updateSingleHostResult(
                            initialItem.copy(status = HostStatus.CHECKING)
                        )

                        val result = hostProbeService.checkHost(initialItem.host, initialItem.group)

                        // Update host result
                        updateSingleHostResult(result)

                        completed++
                        _uiState.update { state ->
                            state.copy(
                                completedCount = completed,
                                progress = if (total > 0) completed.toFloat() / total.toFloat() else 1f
                            )
                        }
                        result
                    }
                }
            }

            val finalResults = deferredResults.awaitAll()

            // Calculate heuristic verdict
            val verdict = evaluateVerdict(finalResults)
            _uiState.update { state ->
                state.copy(
                    isChecking = false,
                    verdict = verdict,
                    progress = 1f
                )
            }

            // Save report to history
            val report = CheckReport(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                networkType = _uiState.value.networkInfo.networkType,
                isVpnActive = _uiState.value.networkInfo.isVpnActive,
                verdictType = verdict.type,
                verdictTitle = verdict.title,
                verdictDescription = verdict.description,
                whitelistTotal = finalResults.count { it.group == HostGroup.WHITELIST },
                whitelistSuccess = finalResults.count { it.group == HostGroup.WHITELIST && isAvailable(it.status) },
                controlTotal = finalResults.count { it.group == HostGroup.CONTROL },
                controlSuccess = finalResults.count { it.group == HostGroup.CONTROL && isAvailable(it.status) },
                customTotal = finalResults.count { it.group == HostGroup.CUSTOM },
                customSuccess = finalResults.count { it.group == HostGroup.CUSTOM && isAvailable(it.status) },
                hostResults = finalResults
            )
            preferencesRepository.saveReport(report)
        }
    }

    fun stopCheck() {
        checkJob?.cancel()
        checkJob = null
        _uiState.update { state ->
            val partialVerdict = if (state.hostResults.any { it.status != HostStatus.IDLE && it.status != HostStatus.CHECKING }) {
                evaluateVerdict(state.hostResults)
            } else null
            state.copy(
                isChecking = false,
                verdict = partialVerdict
            )
        }
    }

    private fun isAvailable(status: HostStatus): Boolean {
        return status == HostStatus.AVAILABLE || status == HostStatus.SLOW
    }

    private fun updateSingleHostResult(updated: HostCheckResult) {
        _uiState.update { state ->
            val updatedList = state.hostResults.map { item ->
                if (item.host == updated.host) updated else item
            }
            state.copy(hostResults = updatedList)
        }
    }

    private fun evaluateVerdict(results: List<HostCheckResult>): Verdict {
        val whitelist = results.filter { it.group == HostGroup.WHITELIST }
        val control = results.filter { it.group == HostGroup.CONTROL }

        val whiteTotal = whitelist.size
        val whiteSuccess = whitelist.count { isAvailable(it.status) }

        val controlTotal = control.size
        val controlSuccess = control.count { isAvailable(it.status) }

        val whiteRatio = if (whiteTotal > 0) whiteSuccess.toFloat() / whiteTotal else 0f
        val controlRatio = if (controlTotal > 0) controlSuccess.toFloat() / controlTotal else 0f

        // 1. Почти все «белые» доступны, почти все контрольные недоступны:
        if (whiteRatio >= 0.70f && controlRatio <= 0.30f) {
            return Verdict(
                title = "Похоже, включён режим белых списков",
                description = "Большинство ресурсов из белого списка доступны ($whiteSuccess из $whiteTotal), а контрольные внешние сервисы заблокированы ($controlSuccess из $controlTotal).",
                type = VerdictType.WHITELIST_ACTIVE
            )
        }

        // 2. Доступно всё:
        if (whiteRatio >= 0.70f && controlRatio >= 0.70f) {
            return Verdict(
                title = "Ограничений не обнаружено",
                description = "Доступны как российские сервисы ($whiteSuccess из $whiteTotal), так и контрольные зарубежные ресурсы ($controlSuccess из $controlTotal).",
                type = VerdictType.NO_RESTRICTIONS
            )
        }

        // 3. Недоступно почти всё:
        if (whiteRatio <= 0.30f && controlRatio <= 0.30f) {
            return Verdict(
                title = "Нет связи или полная блокировка",
                description = "Практически ни один сервис не отвечает ($whiteSuccess/$whiteTotal белых, $controlSuccess/$controlTotal контрольных). Проверьте интернет-соединение.",
                type = VerdictType.NO_CONNECTION
            )
        }

        // 4. Смешанный результат:
        return Verdict(
            title = "Смешанный результат",
            description = "Частичная доступность сервисов ($whiteSuccess/$whiteTotal белых, $controlSuccess/$controlTotal контрольных). Возможно, действуют нестабильная связь или выборочные сбои.",
            type = VerdictType.MIXED
        )
    }

    fun addHost(host: String, group: HostGroup) {
        viewModelScope.launch {
            preferencesRepository.addHost(host, group)
        }
    }

    fun removeHost(host: String, group: HostGroup) {
        viewModelScope.launch {
            preferencesRepository.removeHost(host, group)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            preferencesRepository.resetToDefaults()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            preferencesRepository.clearHistory()
        }
    }

    fun generateShareReportText(customReport: CheckReport? = null): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val state = _uiState.value

        val timeStr: String
        val netType: NetworkType
        val isVpn: Boolean
        val verdictTitle: String
        val verdictDesc: String
        val results: List<HostCheckResult>

        if (customReport != null) {
            timeStr = dateFormat.format(Date(customReport.timestamp))
            netType = customReport.networkType
            isVpn = customReport.isVpnActive
            verdictTitle = customReport.verdictTitle
            verdictDesc = customReport.verdictDescription
            results = customReport.hostResults
        } else {
            timeStr = dateFormat.format(Date())
            netType = state.networkInfo.networkType
            isVpn = state.networkInfo.isVpnActive
            verdictTitle = state.verdict?.title ?: "Проверка не завершена"
            verdictDesc = state.verdict?.description ?: ""
            results = state.hostResults
        }

        val sb = StringBuilder()
        sb.appendLine("📊 Отчёт проверки доступности сети")
        sb.appendLine("Приложение: White List Checker")
        sb.appendLine("Дата: $timeStr")
        sb.appendLine("Тип сети: ${netType.title}")
        sb.appendLine("VPN: ${if (isVpn) "ВКЛЮЧЁН ⚠️" else "Выключен"}")
        sb.appendLine()
        sb.appendLine("⚖️ Итоговый вердикт (эвристика):")
        sb.appendLine("$verdictTitle")
        if (verdictDesc.isNotEmpty()) {
            sb.appendLine(verdictDesc)
        }
        sb.appendLine()

        val whiteGroup = results.filter { it.group == HostGroup.WHITELIST }
        if (whiteGroup.isNotEmpty()) {
            sb.appendLine("🟢 Ожидаемо в белом списке:")
            whiteGroup.forEach { h -> formatHostLine(sb, h) }
            sb.appendLine()
        }

        val controlGroup = results.filter { it.group == HostGroup.CONTROL }
        if (controlGroup.isNotEmpty()) {
            sb.appendLine("🔵 Контрольные хосты:")
            controlGroup.forEach { h -> formatHostLine(sb, h) }
            sb.appendLine()
        }

        val customGroup = results.filter { it.group == HostGroup.CUSTOM }
        if (customGroup.isNotEmpty()) {
            sb.appendLine("🟡 Свои хосты:")
            customGroup.forEach { h -> formatHostLine(sb, h) }
            sb.appendLine()
        }

        sb.appendLine("Проверено с помощью White List Checker")
        return sb.toString()
    }

    private fun formatHostLine(sb: StringBuilder, result: HostCheckResult) {
        val symbol = when (result.status) {
            HostStatus.AVAILABLE -> "✅"
            HostStatus.SLOW -> "⚡"
            HostStatus.TIMEOUT_ERROR -> "⏱️"
            HostStatus.NETWORK_ERROR -> "🚫"
            HostStatus.UNAVAILABLE -> "❌"
            HostStatus.CHECKING -> "⏳"
            HostStatus.IDLE -> "⚪"
        }
        val statusText = when (result.status) {
            HostStatus.AVAILABLE -> "${result.totalLatencyMs} мс (доступен)"
            HostStatus.SLOW -> "${result.totalLatencyMs} мс (медленно)"
            HostStatus.TIMEOUT_ERROR -> "Таймаут (${result.totalLatencyMs} мс: ${result.errorMessage ?: "превышено время ожидания"})"
            HostStatus.NETWORK_ERROR -> {
                val stepName = result.failedStep?.displayName ?: "Сетевой шаг"
                "Ошибка сети на шаге $stepName (${result.errorMessage ?: "ошибка"})"
            }
            HostStatus.UNAVAILABLE -> {
                val stepName = result.failedStep?.displayName ?: "Сбой"
                "Сбой на шаге $stepName (${result.errorMessage ?: "ошибка"})"
            }
            HostStatus.CHECKING -> "проверка..."
            HostStatus.IDLE -> "не проверен"
        }
        sb.appendLine("$symbol ${result.host}: $statusText")
    }
}
