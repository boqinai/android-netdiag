package com.boqinai.android.netdiag

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemInfoTest {
    @Test
    fun reportJsonContainsDeviceAndAppInfo() {
        val report =
            DiagnosticReport(
                startedAtEpochMs = 1,
                durationMs = 2,
                results = emptyList(),
                device =
                    DeviceInfo(
                        manufacturer = "Google",
                        brand = "google",
                        model = "Pixel",
                        device = "pixel",
                        androidVersion = "16",
                        apiLevel = 36,
                        supportedAbis = listOf("arm64-v8a"),
                        screenWidthPixels = 1080,
                        screenHeightPixels = 2400,
                        locale = "zh-CN",
                        timeZone = "Asia/Shanghai",
                        networkType = "wifi",
                    ),
                app =
                    AppInfo(
                        packageName = "com.example.app",
                        appName = "示例",
                        versionName = "1.2.3",
                        versionCode = 12,
                        debuggable = true,
                    ),
            )

        val json = JSONObject(report.toJson())
        assertEquals("Pixel", json.getJSONObject("device").getString("model"))
        assertEquals(
            "arm64-v8a",
            json.getJSONObject("device").getJSONArray("supportedAbis").getString(0),
        )
        assertEquals("1.2.3", json.getJSONObject("app").getString("versionName"))
        assertEquals(true, json.getJSONObject("app").getBoolean("debuggable"))
    }
}
