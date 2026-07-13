package com.boqinai.android.netdiag.demo

import com.boqinai.android.netdiag.ProbeKind
import com.boqinai.android.netdiag.ProbeResult

internal enum class AssessmentLevel(val label: String) {
    NORMAL("正常"),
    SLOW("较慢"),
    ABNORMAL("异常"),
    UNSUPPORTED("设备不支持"),
}

internal data class Assessment(val level: AssessmentLevel)

internal fun assess(result: ProbeResult): Assessment {
    if (!result.success) {
        val unavailable =
            result.kind in setOf(ProbeKind.PING, ProbeKind.TRACEROUTE) &&
                listOf("cannot run program", "no such file", "not found").any {
                    result.error.orEmpty().contains(it, ignoreCase = true)
                }
        return Assessment(
            if (unavailable) AssessmentLevel.UNSUPPORTED else AssessmentLevel.ABNORMAL
        )
    }

    if (result.kind == ProbeKind.PING) {
        val loss =
            Regex("""(\d+(?:\.\d+)?)%\s*packet loss""", RegexOption.IGNORE_CASE)
                .find(result.detail)
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()
        if (loss != null && loss > 10) return Assessment(AssessmentLevel.ABNORMAL)
        if (loss != null && loss > 0) return Assessment(AssessmentLevel.SLOW)
    }

    val slowAfterMs =
        when (result.kind) {
            ProbeKind.DNS,
            ProbeKind.PING,
            ProbeKind.TCP -> 300
            ProbeKind.HTTP,
            ProbeKind.EXTERNAL_IP -> 1_000
            else -> Long.MAX_VALUE
        }
    return Assessment(
        if (result.durationMs > slowAfterMs) AssessmentLevel.SLOW else AssessmentLevel.NORMAL
    )
}
