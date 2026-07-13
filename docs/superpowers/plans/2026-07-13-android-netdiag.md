# Android NetDiag Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a coroutine-first Kotlin Android network-diagnosis SDK and runnable demo.

**Architecture:** A two-module Gradle project exposes immutable config/result models plus one `NetDiag` orchestrator. Small internal probe functions use Android/JDK primitives, emit a deterministic cold Flow, isolate ordinary failures, and preserve coroutine cancellation.

**Tech Stack:** Kotlin, Android SDK, Kotlin coroutines, Gradle Kotlin DSL, JUnit 4, standard Android Views.

## Global Constraints

- Library namespace is `com.boqinai.android.netdiag`; demo namespace is `com.boqinai.android.netdiag.demo`.
- Minimum Android API is 23 and Java bytecode target is 17.
- Runtime dependencies are limited to Kotlin coroutines and Android platform APIs.
- Modules are exactly `netdiag` and `demo`.
- No Java callback API, RTMP client, publishing configuration, Compose, DI, persistence, analytics, or report upload.

---

### Task 1: Buildable project and validated public models

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`
- Create: `netdiag/build.gradle.kts`, `netdiag/src/main/AndroidManifest.xml`
- Test: `netdiag/src/test/kotlin/com/boqinai/android/netdiag/DiagnosticConfigTest.kt`
- Create: `netdiag/src/main/kotlin/com/boqinai/android/netdiag/Models.kt`

**Interfaces:**
- Produces: `DiagnosticConfig`, `ProbeKind`, `ProbeResult`, `DiagnosticReport`, `DiagnosticEvent`.

- [ ] Write a failing JUnit test that constructs a valid config and asserts invalid blank host, port `0`, non-HTTP URL, non-positive timeout/count, and hop limit outside `1..64` throw `IllegalArgumentException`.
- [ ] Run `gradlew.bat :netdiag:testDebugUnitTest --tests "*DiagnosticConfigTest"`; expect compilation failure because `DiagnosticConfig` is absent.
- [ ] Implement data classes and enum in `Models.kt`; put `require` checks in `DiagnosticConfig.init`. Defaults: port `443`, URL `https://example.com`, timeout `5.seconds`, ping count `4`, hop limit `20`, external-IP URL `https://api.ipify.org`.
- [ ] Run the same command; expect PASS.
- [ ] Commit with `git commit -m "feat: add validated diagnosis models"`.

### Task 2: Stable JSON report

**Files:**
- Test: `netdiag/src/test/kotlin/com/boqinai/android/netdiag/DiagnosticReportTest.kt`
- Modify: `netdiag/src/main/kotlin/com/boqinai/android/netdiag/Models.kt`

**Interfaces:**
- Consumes: `DiagnosticReport(startedAtEpochMs: Long, durationMs: Long, results: List<ProbeResult>)`.
- Produces: `fun DiagnosticReport.toJson(): String`.

- [ ] Write a failing test creating one successful DNS result containing a quote/newline and assert `JSONObject(report.toJson())` preserves all fields and `results[0].detail` exactly.
- [ ] Run `gradlew.bat :netdiag:testDebugUnitTest --tests "*DiagnosticReportTest"`; expect unresolved `toJson`.
- [ ] Implement `toJson` using `org.json.JSONObject` and `JSONArray`, explicitly writing report and probe fields; never hand-escape JSON.
- [ ] Re-run the test; expect PASS.
- [ ] Commit with `git commit -m "feat: export diagnosis report as JSON"`.

### Task 3: Probe implementations

**Files:**
- Test: `netdiag/src/test/kotlin/com/boqinai/android/netdiag/ProbeRunnerTest.kt`
- Create: `netdiag/src/main/kotlin/com/boqinai/android/netdiag/Probes.kt`
- Create: `netdiag/src/main/kotlin/com/boqinai/android/netdiag/AndroidNetworkProbe.kt`

**Interfaces:**
- Produces: internal `suspend fun dnsProbe`, `pingProbe`, `tcpProbe`, `traceProbe`, `httpProbe`, `externalIpProbe`, and `fun networkProbe(context)`; each returns one `ProbeResult`.

- [ ] Write failing tests against an internal `runProbe(kind) { ... }`: success returns duration/detail, ordinary exception becomes a failed result, and `CancellationException` is rethrown.
- [ ] Run `gradlew.bat :netdiag:testDebugUnitTest --tests "*ProbeRunnerTest"`; expect unresolved `runProbe`.
- [ ] Implement `runProbe` with `System.nanoTime`, rethrow cancellation, and map other exceptions to failure. Implement DNS with `InetAddress.getAllByName`, TCP with `Socket.connect`, HTTP/external IP with `HttpURLConnection`, commands with `ProcessBuilder`, bounded timeouts/output, and cleanup in `finally`. Implement network info with `ConnectivityManager`/`LinkProperties`.
- [ ] Re-run all library tests; expect PASS.
- [ ] Commit with `git commit -m "feat: add Android network probes"`.

### Task 4: Coroutine orchestration and deterministic events

**Files:**
- Test: `netdiag/src/test/kotlin/com/boqinai/android/netdiag/NetDiagTest.kt`
- Create: `netdiag/src/main/kotlin/com/boqinai/android/netdiag/NetDiag.kt`

**Interfaces:**
- Produces: `class NetDiag(context: Context)`, `fun events(config: DiagnosticConfig): Flow<DiagnosticEvent>`, and `suspend fun run(config: DiagnosticConfig): DiagnosticReport`.

- [ ] Write a failing test using an internal constructor accepting seven probe lambdas; assert start/result pairs occur in NETWORK, DNS, PING, TCP, TRACEROUTE, HTTP, EXTERNAL_IP order and one probe failure does not skip later probes.
- [ ] Add a failing cancellation test whose first lambda suspends forever; cancel collection and assert later lambdas never run.
- [ ] Run `gradlew.bat :netdiag:testDebugUnitTest --tests "*NetDiagTest"`; expect unresolved `NetDiag`.
- [ ] Implement a cold `flow`, sequential `withContext(Dispatchers.IO)` calls, final report emission, and `run` by collecting the final event. Do not catch cancellation.
- [ ] Run `gradlew.bat :netdiag:testDebugUnitTest`; expect PASS.
- [ ] Commit with `git commit -m "feat: orchestrate coroutine diagnosis flow"`.

### Task 5: Demo and end-to-end build

**Files:**
- Create: `demo/build.gradle.kts`, `demo/src/main/AndroidManifest.xml`
- Create: `demo/src/main/res/layout/activity_main.xml`, `demo/src/main/res/values/strings.xml`, `demo/src/main/res/values/themes.xml`
- Create: `demo/src/main/kotlin/com/boqinai/android/netdiag/demo/MainActivity.kt`
- Create: `README.md`

**Interfaces:**
- Consumes: public `DiagnosticConfig`, `NetDiag.events`, and `DiagnosticReport.toJson`.

- [ ] Add the demo module with INTERNET and ACCESS_NETWORK_STATE permissions and a standard-view form for host, port, URL, external-IP endpoint, Run, Cancel, and scrolling output.
- [ ] Implement `MainActivity`: validate fields through `DiagnosticConfig`, launch collection in `lifecycleScope`, append each event, render final JSON, cancel the active Job, and cancel it in `onDestroy`.
- [ ] Document Gradle dependency usage, SDK sample code, probe behavior/limitations, permissions, and demo build/install commands in `README.md`.
- [ ] Run `gradlew.bat :netdiag:testDebugUnitTest :demo:assembleDebug lint`; expect BUILD SUCCESSFUL with no errors.
- [ ] If `adb devices` lists a device, install the APK, launch the activity, run once, then test Cancel. Otherwise record device validation as not performed.
- [ ] Run `git diff --check` and `git status --short`; expect no whitespace errors and only intended files.
- [ ] Commit with `git commit -m "feat: add network diagnosis demo"`.
