package com.boqinai.android.netdiag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PingMetricsTest {
    @Test
    fun parsesAndroidPingSummary() {
        val output = """
            4 packets transmitted, 4 received, 0% packet loss, time 3004ms
            rtt min/avg/max/mdev = 10.100/20.200/35.300/9.400 ms
        """.trimIndent()

        val parsed = parsePingMetrics(output)
        assertNotNull(parsed)
        val metrics = parsed!!
        assertEquals(10.1, metrics.minMs, 0.001)
        assertEquals(20.2, metrics.avgMs, 0.001)
        assertEquals(35.3, metrics.maxMs, 0.001)
        assertEquals(9.4, metrics.jitterMs, 0.001)
        assertEquals(0.0, metrics.packetLossPercent, 0.001)
    }
}
