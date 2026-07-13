# Android NetDiag

Coroutine-first Android network diagnosis SDK (`minSdk 23`). It reports network state, DNS, ping, TCP connect, traceroute, HTTP(S), and external IP as structured results.

```kotlin
val report = NetDiag(context).run(DiagnosticConfig("example.com"))
println(report.toJson())

NetDiag(context).events(DiagnosticConfig("example.com")).collect { event ->
    // update progress UI
}
```

Add the `netdiag` module as a dependency and declare `INTERNET` plus `ACCESS_NETWORK_STATE`. Cancellation follows the collecting coroutine. Ping/traceroute depend on commands provided by the Android image; unsupported commands become failed probe results and do not stop the run.

Build and run the demo:

```powershell
.\gradlew.bat :demo:assembleDebug
adb install -r demo\build\outputs\apk\debug\demo-debug.apk
```
