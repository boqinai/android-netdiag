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
    fun fastSuccessfulProbeIsNormal() {
        val result = ProbeResult(ProbeKind.TCP, true, 100, "connected")
        assertEquals(AssessmentLevel.NORMAL, assess(result).level)
    }
}
