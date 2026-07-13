package com.boqinai.android.netdiag

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.json.JSONArray
import org.json.JSONObject

data class DiagnosticConfig(
    val host: String,
    val port: Int = 443,
    val url: String = "https://www.baidu.com",
    val timeout: Duration = 5.seconds,
    val pingCount: Int = 4,
    val maxHops: Int = 20,
    val externalIpUrl: String = "https://www.boqinai.com",
) {
    init {
        require(host.isNotBlank())
        require(port in 1..65535)
        require(url.startsWith("http://") || url.startsWith("https://"))
        require(timeout.isPositive())
        require(pingCount > 0)
        require(maxHops in 1..64)
        require(externalIpUrl.startsWith("https://"))
    }
}

enum class ProbeKind {
    NETWORK,
    DNS,
    PING,
    TCP,
    TRACEROUTE,
    HTTP,
    EXTERNAL_IP,
}

data class ProbeResult(
    val kind: ProbeKind,
    val success: Boolean,
    val durationMs: Long,
    val detail: String,
    val error: String? = null,
)

data class DeviceInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val androidVersion: String,
    val apiLevel: Int,
    val supportedAbis: List<String>,
    val screenWidthPixels: Int,
    val screenHeightPixels: Int,
    val locale: String,
    val timeZone: String,
    val networkType: String,
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val debuggable: Boolean,
)

data class DiagnosticReport(
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val results: List<ProbeResult>,
    val device: DeviceInfo? = null,
    val app: AppInfo? = null,
) {
    fun toJson(): String =
        JSONObject()
            .put("startedAtEpochMs", startedAtEpochMs)
            .put("durationMs", durationMs)
            .put("device", device?.toJson() ?: JSONObject.NULL)
            .put("app", app?.toJson() ?: JSONObject.NULL)
            .put(
                "results",
                JSONArray(
                    results.map {
                        JSONObject()
                            .put("kind", it.kind.name)
                            .put("success", it.success)
                            .put("durationMs", it.durationMs)
                            .put("detail", it.detail)
                            .put("error", it.error ?: JSONObject.NULL)
                    }
                ),
            )
            .toString(2)
}

private fun DeviceInfo.toJson() =
    JSONObject()
        .put("manufacturer", manufacturer)
        .put("brand", brand)
        .put("model", model)
        .put("device", device)
        .put("androidVersion", androidVersion)
        .put("apiLevel", apiLevel)
        .put("supportedAbis", JSONArray(supportedAbis))
        .put("screenWidthPixels", screenWidthPixels)
        .put("screenHeightPixels", screenHeightPixels)
        .put("locale", locale)
        .put("timeZone", timeZone)
        .put("networkType", networkType)

private fun AppInfo.toJson() =
    JSONObject()
        .put("packageName", packageName)
        .put("appName", appName)
        .put("versionName", versionName)
        .put("versionCode", versionCode)
        .put("debuggable", debuggable)

sealed interface DiagnosticEvent {
    data class Started(val kind: ProbeKind) : DiagnosticEvent

    data class Completed(val result: ProbeResult) : DiagnosticEvent

    data class Finished(val report: DiagnosticReport) : DiagnosticEvent
}
