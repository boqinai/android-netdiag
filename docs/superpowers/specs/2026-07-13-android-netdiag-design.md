# Android NetDiag Kotlin SDK Design

## Goal

Create `G:\New_Workspace\android-netdiag`, a Kotlin-first Android network-diagnosis SDK with a runnable demo app. It modernizes the capabilities of `qiniu/android-netdiag` without preserving its Java API.

## Project shape

- `netdiag`: Android library, namespace `com.boqinai.android.netdiag`.
- `demo`: Android application, namespace `com.boqinai.android.netdiag.demo`.
- Minimum Android API 23, Java 17 bytecode.
- Gradle Wrapper included; Kotlin DSL build files.
- Runtime dependencies limited to Kotlin coroutines and Android platform APIs.

## Public API

The primary API is coroutine-first:

```kotlin
val report = NetDiag(context).run(config)
NetDiag(context).events(config).collect { event -> ... }
```

`run` is a suspending convenience method returning the final `DiagnosticReport`. `events` returns a cold `Flow<DiagnosticEvent>` that emits stage start/result events and a final report. Cancelling either coroutine stops pending work and closes sockets/connections.

`DiagnosticConfig` contains the target host, TCP port, HTTP/HTTPS URL, per-probe timeout, ping count, traceroute hop limit, and optional external-IP endpoint. Construction validates host, port, URL scheme, positive timeout/count, and hop limit before network work starts.

The result model records timestamps, duration, success/failure, and error text for each probe. `DiagnosticReport.toJson()` uses Android's built-in `org.json`; no serialization library is added.

## Probes

The complete run executes these stages in a deterministic order so demo output and uploaded reports are readable:

1. Current Android network type, capabilities, local addresses, DNS servers, and proxy.
2. DNS lookup through `InetAddress`.
3. ICMP-style ping through Android's system `ping` executable, capturing latency, loss, output, timeout, and unsupported-command failure.
4. TCP connect timing through `Socket.connect`.
5. Traceroute through the system `traceroute`/`toybox traceroute` command when available; unavailability is a recorded unsupported result, not a fatal run error.
6. HTTP/HTTPS timing and status through `HttpURLConnection`, without consuming or storing the response body.
7. External IP lookup from a configurable HTTPS endpoint, with a small bounded response read.

Each blocking operation runs on `Dispatchers.IO`. Probe failures are isolated: cancellation propagates, while ordinary exceptions become failed probe results and the remaining stages continue.

## Demo

The demo has one screen built with standard Android views. It provides editable host, port, URL, and external-IP endpoint fields, plus Run and Cancel buttons. A scrolling text area shows progress events and the final JSON report. The screen handles lifecycle cancellation and disables conflicting actions while a run is active.

Defaults use public, replaceable targets suitable for a first run. The demo declares internet and network-state permissions.

## Testing and verification

Development follows red-green-refactor for non-trivial behavior. JVM unit tests cover configuration validation, JSON output, probe-failure isolation, and event ordering using injectable probe functions kept internal to the library. Android-dependent network inspection receives an instrumented smoke test where practical.

Completion requires:

- `gradlew.bat :netdiag:testDebugUnitTest`
- `gradlew.bat :demo:assembleDebug`
- `gradlew.bat lint`

If an emulator/device is available, install and launch the demo and verify one complete diagnosis plus cancellation. Otherwise, explicitly report that device-level ICMP/traceroute behavior remains unverified.

## Explicit non-goals

- Compatibility with the old Java callback API.
- RTMP-specific probing; modern RTMP diagnosis needs a separate protocol client and is not justified by the requested general network SDK.
- Maven Central or private-repository publishing configuration.
- Dependency injection, Compose, persistence, analytics, or automatic report upload.
- Parsing vendor-specific `ping`/`traceroute` output into a rigid cross-device schema beyond stable summary fields and raw output.

These can be added later only when a concrete consumer requires them.
