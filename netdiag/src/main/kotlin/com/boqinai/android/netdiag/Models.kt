package com.boqinai.android.netdiag

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.json.JSONArray
import org.json.JSONObject

data class DiagnosticConfig(
    val host: String,
    val port: Int = 443,
    val url: String = "https://example.com",
    val timeout: Duration = 5.seconds,
    val pingCount: Int = 4,
    val maxHops: Int = 20,
    val externalIpUrl: String = "https://api.ipify.org",
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

data class DiagnosticReport(
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val results: List<ProbeResult>,
) {
    fun toJson(): String =
        JSONObject()
            .put("startedAtEpochMs", startedAtEpochMs)
            .put("durationMs", durationMs)
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

sealed interface DiagnosticEvent {
    data class Started(val kind: ProbeKind) : DiagnosticEvent

    data class Completed(val result: ProbeResult) : DiagnosticEvent

    data class Finished(val report: DiagnosticReport) : DiagnosticEvent
}
