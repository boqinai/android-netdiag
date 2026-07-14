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

internal fun displayDurationMs(level: AssessmentLevel, result: ProbeResult): Long? {
    if (level == AssessmentLevel.UNSUPPORTED) return null

    if (result.kind == ProbeKind.PING) {
        return AVG_REGEX.find(result.detail)?.groupValues?.get(1)?.toDoubleOrNull()?.toLong()
    }
    return result.durationMs
}

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

    if (
        result.kind == ProbeKind.NETWORK &&
            ("captivePortal=true" in result.detail || "validated=false" in result.detail)
    ) {
        return Assessment(AssessmentLevel.ABNORMAL)
    }

    if (result.kind == ProbeKind.PING) {
        val loss =
            LOSS_REGEX.find(result.detail)
                ?.groupValues
                ?.drop(1)
                ?.firstNotNullOfOrNull(String::toDoubleOrNull)
        val average = AVG_REGEX.find(result.detail)?.groupValues?.get(1)?.toDoubleOrNull()
        if (loss != null && loss > 10) return Assessment(AssessmentLevel.ABNORMAL)
        if (loss != null && loss > 0) return Assessment(AssessmentLevel.SLOW)
        if (average != null) {
            return Assessment(if (average > 300) AssessmentLevel.SLOW else AssessmentLevel.NORMAL)
        }
    }

    val slowAfterMs =
        when (result.kind) {
            ProbeKind.DNS,
            ProbeKind.TCP,
            ProbeKind.IPV4,
            ProbeKind.IPV6 -> 300
            ProbeKind.HTTP,
            ProbeKind.EXTERNAL_IP -> 1_000
            else -> Long.MAX_VALUE
        }
    return Assessment(
        if (result.durationMs > slowAfterMs) AssessmentLevel.SLOW else AssessmentLevel.NORMAL
    )
}

internal fun abnormalDetailRanges(json: String, results: List<ProbeResult>): List<IntRange> =
    buildList {
        var searchFrom = 0
        for (result in results) {
            val marker = "\"kind\": \"" + result.kind.name + "\""
            var blockStart = json.indexOf(marker, searchFrom)
            if (blockStart < 0) {
                blockStart = json.indexOf("\"kind\":\"" + result.kind.name + "\"", searchFrom)
            }
            if (blockStart < 0) continue
            searchFrom = blockStart + marker.length
            if (assess(result).level != AssessmentLevel.ABNORMAL) continue
            val blockEnd = json.indexOf("\"kind\"", searchFrom).takeIf { it >= 0 } ?: json.length
            listOf(result.detail, result.error.orEmpty())
                .filter(String::isNotEmpty)
                .forEach { value ->
                    val encoded = escapeJson(value)
                    val start = json.indexOf(encoded, blockStart)
                    if (start in blockStart until blockEnd) add(start until start + encoded.length)
                }
        }
    }

private fun escapeJson(value: String): String =
    buildString {
        value.forEach {
            append(
                when (it) {
                    '\\' -> "\\\\"
                    '"' -> "\\\""
                    '\n' -> "\\n"
                    '\r' -> "\\r"
                    '\t' -> "\\t"
                    else -> it
                }
            )
        }
    }

private val LOSS_REGEX =
    Regex(
        """(?:packetLossPercent=(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)%\s*packet loss)""",
        RegexOption.IGNORE_CASE,
    )
private val AVG_REGEX = Regex("""avgMs=([\d.]+)""")
