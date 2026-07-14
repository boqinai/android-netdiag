package com.boqinai.android.netdiag.demo

import com.boqinai.android.netdiag.ProbeKind
import com.boqinai.android.netdiag.ProbeResult
import org.junit.Assert.assertEquals
import org.junit.Test

class AssessmentTest {
    @Test
    fun unavailableCommandIsUnsupported() {
        val result =
            ProbeResult(ProbeKind.TRACEROUTE, false, 1, "", "Cannot run program traceroute")
        assertEquals(AssessmentLevel.UNSUPPORTED, assess(result).level)
    }

    @Test
    fun unsupportedProbeHidesDuration() {
        val result = ProbeResult(ProbeKind.TRACEROUTE, false, 12, "", "not found")
        assertEquals(null, displayDurationMs(AssessmentLevel.UNSUPPORTED, result))
    }

    @Test
    fun pingDisplaysAverageRttInsteadOfCommandDuration() {
        val result =
            ProbeResult(
                ProbeKind.PING,
                true,
                3_000,
                "minMs=10.1, avgMs=20.2, maxMs=35.3, jitterMs=9.4, packetLossPercent=0.0",
            )
        assertEquals(20L, displayDurationMs(AssessmentLevel.NORMAL, result))
        assertEquals(AssessmentLevel.NORMAL, assess(result).level)
    }

    @Test
    fun captivePortalIsAbnormal() {
        val result =
            ProbeResult(
                ProbeKind.NETWORK,
                true,
                1,
                "type=wifi, validated=false, captivePortal=true, vpn=false, metered=false",
            )
        assertEquals(AssessmentLevel.ABNORMAL, assess(result).level)
    }

    @Test
    fun failedProbeIsAbnormal() {
        val result = ProbeResult(ProbeKind.DNS, false, 100, "", "unknown host")
        assertEquals(AssessmentLevel.ABNORMAL, assess(result).level)
    }

    @Test
    fun slowSuccessfulProbeIsSlow() {
        val result = ProbeResult(ProbeKind.HTTP, true, 1500, "HTTP 200")
        assertEquals(AssessmentLevel.SLOW, assess(result).level)
    }

    @Test
    fun highPingPacketLossIsAbnormal() {
        val result =
            ProbeResult(
                ProbeKind.PING,
                true,
                100,
                "4 packets transmitted, 3 received, 25% packet loss",
            )
        assertEquals(AssessmentLevel.ABNORMAL, assess(result).level)
    }

    @Test
    fun findsOnlyAbnormalDetailValues() {
        val abnormal = ProbeResult(ProbeKind.DNS, false, 1, "", "unknown host")
        val normal = ProbeResult(ProbeKind.TCP, true, 1, "connected")
        val json = """{"kind":"DNS","error":"unknown host"},{"kind":"TCP","detail":"connected"}"""

        val highlighted = abnormalDetailRanges(json, listOf(abnormal, normal)).map { json.substring(it) }

        assertEquals(listOf("unknown host"), highlighted)
    }

    @Test
    fun fastSuccessfulProbeIsNormal() {
        val result = ProbeResult(ProbeKind.TCP, true, 100, "connected")
        assertEquals(AssessmentLevel.NORMAL, assess(result).level)
    }
}
