package com.boqinai.android.netdiag

internal data class PingMetrics(
    val minMs: Double,
    val avgMs: Double,
    val maxMs: Double,
    val jitterMs: Double,
    val packetLossPercent: Double,
) {
    fun detail(): String =
        "minMs=$minMs, avgMs=$avgMs, maxMs=$maxMs, jitterMs=$jitterMs, " +
            "packetLossPercent=$packetLossPercent"
}

internal fun parsePingMetrics(output: String): PingMetrics? {
    val times =
        Regex("""(?:rtt|round-trip)[^=]*=\s*([\d.]+)/([\d.]+)/([\d.]+)/([\d.]+)\s*ms""")
            .find(output)
            ?.groupValues
            ?.drop(1)
            ?.mapNotNull(String::toDoubleOrNull)
            ?.takeIf { it.size == 4 }
            ?: return null
    val loss =
        Regex("""([\d.]+)%\s*packet loss""", RegexOption.IGNORE_CASE)
            .find(output)
            ?.groupValues
            ?.get(1)
            ?.toDoubleOrNull()
            ?: return null
    return PingMetrics(times[0], times[1], times[2], times[3], loss)
}
