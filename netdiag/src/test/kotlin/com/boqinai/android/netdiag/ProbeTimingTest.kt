package com.boqinai.android.netdiag

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbeTimingTest {
    @Test
    fun successfulProbeIncludesBlockDuration() = runBlocking {
        val result =
            runProbe(ProbeKind.DNS) {
                delay(100.milliseconds)
                "ok"
            }

        assertTrue("durationMs=${result.durationMs}", result.durationMs >= 80)
    }
}
