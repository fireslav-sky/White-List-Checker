package com.example.model

enum class HostGroup(val displayName: String, val description: String) {
    WHITELIST("Ожидаемо в белом списке", "Российские сервисы, банки, госуслуги"),
    CONTROL("Контрольные", "Зарубежные сервисы, обычно вне белого списка"),
    CUSTOM("Свои хосты", "Пользовательские адреса для проверки")
}

enum class CheckStep(val displayName: String) {
    DNS("DNS-резолв"),
    TCP("TCP (порт 443)"),
    TLS("TLS-рукопожатие"),
    HTTP("HTTP запрос")
}

data class StepResult(
    val step: CheckStep,
    val isSuccess: Boolean,
    val latencyMs: Long,
    val details: String
)

enum class HostStatus(val displayName: String) {
    IDLE("Ожидание"),
    CHECKING("Проверка..."),
    AVAILABLE("Доступен"),           // All passed, <= 2000 ms
    SLOW("Медленно"),                // All passed, > 2000 ms
    NETWORK_ERROR("Ошибка сети"),     // Network error (reset, connection refused, no route, etc.)
    TIMEOUT_ERROR("Ошибка таймаута"), // Timeout (ConnectTimeout or ReadTimeout > 5000 ms)
    UNAVAILABLE("Недоступен")        // General unavailable
}

data class HostCheckResult(
    val host: String,
    val group: HostGroup,
    val status: HostStatus = HostStatus.IDLE,
    val totalLatencyMs: Long = 0L,
    val failedStep: CheckStep? = null,
    val errorMessage: String? = null,
    val stepResults: List<StepResult> = emptyList()
)

data class HostItem(
    val host: String,
    val group: HostGroup
)

enum class NetworkType(val title: String) {
    CELLULAR("Мобильный интернет"),
    WIFI("Wi-Fi подключение"),
    ETHERNET("Проводная сеть (Ethernet)"),
    NO_NETWORK("Нет подключения"),
    OTHER("Другая сеть")
}

data class NetworkStatusInfo(
    val networkType: NetworkType,
    val isVpnActive: Boolean,
    val networkDescription: String
)

enum class VerdictType {
    PENDING,
    WHITELIST_ACTIVE,
    NO_RESTRICTIONS,
    NO_CONNECTION,
    MIXED
}

data class Verdict(
    val title: String,
    val description: String,
    val type: VerdictType
)

data class CheckReport(
    val id: String,
    val timestamp: Long,
    val networkType: NetworkType,
    val isVpnActive: Boolean,
    val verdictType: VerdictType,
    val verdictTitle: String,
    val verdictDescription: String,
    val whitelistTotal: Int,
    val whitelistSuccess: Int,
    val controlTotal: Int,
    val controlSuccess: Int,
    val customTotal: Int,
    val customSuccess: Int,
    val hostResults: List<HostCheckResult>
)
