package com.boqinai.android.netdiag
import org.junit.Assert.*
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
class DiagnosticConfigTest { @Test fun validatesInput() { assertEquals(443, DiagnosticConfig("example.com").port); listOf<() -> Unit>({ DiagnosticConfig(" ") }, { DiagnosticConfig("x", port=0) }, { DiagnosticConfig("x", url="ftp://x") }, { DiagnosticConfig("x", timeout=0.seconds) }, { DiagnosticConfig("x", pingCount=0) }, { DiagnosticConfig("x", maxHops=65) }).forEach { assertThrows(IllegalArgumentException::class.java, it) } } }
