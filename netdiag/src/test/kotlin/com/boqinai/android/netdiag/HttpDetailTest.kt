package com.boqinai.android.netdiag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HttpDetailTest {
    @Test
    fun containsOnlyUrlAndStatusCode() {
        val detail = httpDetail("https://www.boqinai.com", 200)

        assertEquals("url=https://www.boqinai.com, status=200", detail)
        assertFalse(detail.contains("html", ignoreCase = true))
    }
}
