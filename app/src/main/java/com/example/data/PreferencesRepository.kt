package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CheckReport
import com.example.model.CheckStep
import com.example.model.HostCheckResult
import com.example.model.HostGroup
import com.example.model.HostItem
import com.example.model.HostStatus
import com.example.model.NetworkType
import com.example.model.StepResult
import com.example.model.VerdictType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "whitelist_checker_prefs")

class PreferencesRepository(private val context: Context) {

    companion object {
        val KEY_WHITELIST = stringPreferencesKey("hosts_whitelist")
        val KEY_CONTROL = stringPreferencesKey("hosts_control")
        val KEY_CUSTOM = stringPreferencesKey("hosts_custom")
        val KEY_HISTORY = stringPreferencesKey("reports_history_json")

        val DEFAULT_WHITELIST = listOf(
            "ya.ru",
            "yandex.ru",
            "sberbank.ru",
            "tbank.ru",
            "vtb.ru",
            "gosuslugi.ru",
            "vk.com",
            "mail.ru",
            "ozon.ru",
            "wildberries.ru",
            "2gis.ru",
            "rutube.ru"
        )

        val DEFAULT_CONTROL = listOf(
            "google.com",
            "wikipedia.org",
            "github.com",
            "cloudflare.com",
            "telegram.org"
        )
    }

    val hostItemsFlow: Flow<List<HostItem>> = context.dataStore.data.map { prefs ->
        val whitelistStr = prefs[KEY_WHITELIST]
        val controlStr = prefs[KEY_CONTROL]
        val customStr = prefs[KEY_CUSTOM]

        val whitelist = if (whitelistStr != null) {
            parseList(whitelistStr)
        } else {
            DEFAULT_WHITELIST
        }

        val control = if (controlStr != null) {
            parseList(controlStr)
        } else {
            DEFAULT_CONTROL
        }

        val custom = if (customStr != null) {
            parseList(customStr)
        } else {
            emptyList()
        }

        val items = mutableListOf<HostItem>()
        whitelist.forEach { items.add(HostItem(it, HostGroup.WHITELIST)) }
        control.forEach { items.add(HostItem(it, HostGroup.CONTROL)) }
        custom.forEach { items.add(HostItem(it, HostGroup.CUSTOM)) }
        items
    }

    val historyReportsFlow: Flow<List<CheckReport>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_HISTORY] ?: "[]"
        deserializeReports(jsonStr)
    }

    suspend fun addHost(rawHost: String, group: HostGroup) {
        val clean = rawHost.trim().lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .split("/")[0]
        if (clean.isEmpty()) return

        context.dataStore.edit { prefs ->
            val key = when (group) {
                HostGroup.WHITELIST -> KEY_WHITELIST
                HostGroup.CONTROL -> KEY_CONTROL
                HostGroup.CUSTOM -> KEY_CUSTOM
            }
            val currentList = prefs[key]?.let { parseList(it) } ?: when (group) {
                HostGroup.WHITELIST -> DEFAULT_WHITELIST
                HostGroup.CONTROL -> DEFAULT_CONTROL
                HostGroup.CUSTOM -> emptyList()
            }

            if (!currentList.contains(clean)) {
                val updated = currentList + clean
                prefs[key] = serializeList(updated)
            }
        }
    }

    suspend fun removeHost(host: String, group: HostGroup) {
        val clean = host.trim().lowercase()
        context.dataStore.edit { prefs ->
            val key = when (group) {
                HostGroup.WHITELIST -> KEY_WHITELIST
                HostGroup.CONTROL -> KEY_CONTROL
                HostGroup.CUSTOM -> KEY_CUSTOM
            }
            val currentList = prefs[key]?.let { parseList(it) } ?: when (group) {
                HostGroup.WHITELIST -> DEFAULT_WHITELIST
                HostGroup.CONTROL -> DEFAULT_CONTROL
                HostGroup.CUSTOM -> emptyList()
            }

            val updated = currentList.filter { it != clean }
            prefs[key] = serializeList(updated)
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            prefs[KEY_WHITELIST] = serializeList(DEFAULT_WHITELIST)
            prefs[KEY_CONTROL] = serializeList(DEFAULT_CONTROL)
            prefs[KEY_CUSTOM] = serializeList(emptyList())
        }
    }

    suspend fun saveReport(report: CheckReport) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[KEY_HISTORY] ?: "[]"
            val existing = deserializeReports(currentJson).toMutableList()
            // Add new report at the beginning, limit to last 20
            existing.add(0, report)
            val trimmed = existing.take(20)
            prefs[KEY_HISTORY] = serializeReports(trimmed)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs[KEY_HISTORY] = "[]"
        }
    }

    private fun parseList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        val jsonArray = JSONArray(value)
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }

    private fun serializeList(list: List<String>): String {
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    private fun serializeReports(reports: List<CheckReport>): String {
        val array = JSONArray()
        for (report in reports) {
            val obj = JSONObject()
            obj.put("id", report.id)
            obj.put("timestamp", report.timestamp)
            obj.put("networkType", report.networkType.name)
            obj.put("isVpnActive", report.isVpnActive)
            obj.put("verdictType", report.verdictType.name)
            obj.put("verdictTitle", report.verdictTitle)
            obj.put("verdictDescription", report.verdictDescription)
            obj.put("whitelistTotal", report.whitelistTotal)
            obj.put("whitelistSuccess", report.whitelistSuccess)
            obj.put("controlTotal", report.controlTotal)
            obj.put("controlSuccess", report.controlSuccess)
            obj.put("customTotal", report.customTotal)
            obj.put("customSuccess", report.customSuccess)

            val hostsArray = JSONArray()
            for (h in report.hostResults) {
                val hObj = JSONObject()
                hObj.put("host", h.host)
                hObj.put("group", h.group.name)
                hObj.put("status", h.status.name)
                hObj.put("totalLatencyMs", h.totalLatencyMs)
                hObj.put("failedStep", h.failedStep?.name ?: "")
                hObj.put("errorMessage", h.errorMessage ?: "")

                val stepsArray = JSONArray()
                for (s in h.stepResults) {
                    val sObj = JSONObject()
                    sObj.put("step", s.step.name)
                    sObj.put("isSuccess", s.isSuccess)
                    sObj.put("latencyMs", s.latencyMs)
                    sObj.put("details", s.details)
                    stepsArray.put(sObj)
                }
                hObj.put("stepResults", stepsArray)
                hostsArray.put(hObj)
            }
            obj.put("hostResults", hostsArray)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeReports(json: String): List<CheckReport> {
        val list = mutableListOf<CheckReport>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "")
                val timestamp = obj.optLong("timestamp", 0L)
                val networkType = try {
                    NetworkType.valueOf(obj.optString("networkType", NetworkType.NO_NETWORK.name))
                } catch (_: Exception) {
                    NetworkType.NO_NETWORK
                }
                val isVpnActive = obj.optBoolean("isVpnActive", false)
                val verdictType = try {
                    VerdictType.valueOf(obj.optString("verdictType", VerdictType.PENDING.name))
                } catch (_: Exception) {
                    VerdictType.PENDING
                }
                val verdictTitle = obj.optString("verdictTitle", "")
                val verdictDescription = obj.optString("verdictDescription", "")
                val whitelistTotal = obj.optInt("whitelistTotal", 0)
                val whitelistSuccess = obj.optInt("whitelistSuccess", 0)
                val controlTotal = obj.optInt("controlTotal", 0)
                val controlSuccess = obj.optInt("controlSuccess", 0)
                val customTotal = obj.optInt("customTotal", 0)
                val customSuccess = obj.optInt("customSuccess", 0)

                val hostResults = mutableListOf<HostCheckResult>()
                val hostsArray = obj.optJSONArray("hostResults")
                if (hostsArray != null) {
                    for (j in 0 until hostsArray.length()) {
                        val hObj = hostsArray.getJSONObject(j)
                        val host = hObj.optString("host", "")
                        val group = try {
                            HostGroup.valueOf(hObj.optString("group", HostGroup.WHITELIST.name))
                        } catch (_: Exception) {
                            HostGroup.WHITELIST
                        }
                        val status = try {
                            HostStatus.valueOf(hObj.optString("status", HostStatus.IDLE.name))
                        } catch (_: Exception) {
                            HostStatus.IDLE
                        }
                        val totalLatencyMs = hObj.optLong("totalLatencyMs", 0L)
                        val failedStepStr = hObj.optString("failedStep", "")
                        val failedStep = if (failedStepStr.isNotEmpty()) {
                            try { CheckStep.valueOf(failedStepStr) } catch (_: Exception) { null }
                        } else null
                        val errorMessage = hObj.optString("errorMessage").takeIf { it.isNotEmpty() }

                        val steps = mutableListOf<StepResult>()
                        val stepsArray = hObj.optJSONArray("stepResults")
                        if (stepsArray != null) {
                            for (k in 0 until stepsArray.length()) {
                                val sObj = stepsArray.getJSONObject(k)
                                val step = try {
                                    CheckStep.valueOf(sObj.optString("step", CheckStep.DNS.name))
                                } catch (_: Exception) {
                                    CheckStep.DNS
                                }
                                val isSuccess = sObj.optBoolean("isSuccess", false)
                                val latencyMs = sObj.optLong("latencyMs", 0L)
                                val details = sObj.optString("details", "")
                                steps.add(StepResult(step, isSuccess, latencyMs, details))
                            }
                        }

                        hostResults.add(
                            HostCheckResult(
                                host = host,
                                group = group,
                                status = status,
                                totalLatencyMs = totalLatencyMs,
                                failedStep = failedStep,
                                errorMessage = errorMessage,
                                stepResults = steps
                            )
                        )
                    }
                }

                list.add(
                    CheckReport(
                        id = id,
                        timestamp = timestamp,
                        networkType = networkType,
                        isVpnActive = isVpnActive,
                        verdictType = verdictType,
                        verdictTitle = verdictTitle,
                        verdictDescription = verdictDescription,
                        whitelistTotal = whitelistTotal,
                        whitelistSuccess = whitelistSuccess,
                        controlTotal = controlTotal,
                        controlSuccess = controlSuccess,
                        customTotal = customTotal,
                        customSuccess = customSuccess,
                        hostResults = hostResults
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }
}
