# Android NetDiag

[中文说明](README_CN.md)

Coroutine-first Android network diagnosis SDK (`minSdk 23`). It reports network state, DNS, ping, TCP connect, IPv4/IPv6 reachability, traceroute, HTTP(S), and external IP as structured results, together with basic device, system, and application information.

```kotlin
val report = NetDiag(context).run(DiagnosticConfig("baidu.com"))
println(report.toJson())

NetDiag(context).events(DiagnosticConfig("baidu.com")).collect { event ->
    // update progress UI
}
```

Add the `netdiag` module as a dependency and declare `INTERNET` plus `ACCESS_NETWORK_STATE`. Cancellation follows the collecting coroutine. Ping/traceroute depend on commands provided by the Android image; unsupported commands become failed probe results and do not stop the run.

Ping details include minimum, average, and maximum RTT, jitter, and packet loss. HTTPS details include DNS, TCP, TLS, time-to-first-byte, and total timings. Network status also reports validation, captive portal, VPN, and metered flags, while IPv4 and IPv6 connectivity are checked independently against the configured port.

Build and run the demo:

```powershell
.\gradlew.bat :demo:assembleDebug
adb install -r demo\build\outputs\apk\debug\demo-debug.apk
```

## Basic information

`DiagnosticReport.device` includes manufacturer, brand, model, device name, Android version/API level, CPU ABIs, screen resolution, locale, time zone, and network type. `DiagnosticReport.app` includes app name, package name, version name/code, and debug/release state.

No additional permission is required. The SDK does not collect IMEI, Android ID, phone number, MAC address, serial number, or other unique device identifiers.