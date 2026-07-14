package com.boqinai.android.netdiag

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal suspend fun runProbe(kind: ProbeKind, block: suspend () -> String): ProbeResult {
    val start = System.nanoTime()
    return try {
        val detail = block()
        ProbeResult(kind, true, (System.nanoTime() - start) / 1_000_000, detail)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ProbeResult(
            kind,
            false,
            (System.nanoTime() - start) / 1_000_000,
            "",
            e.message ?: e.javaClass.simpleName,
        )
    }
}

internal suspend fun dnsProbe(c: DiagnosticConfig) =
    runProbe(ProbeKind.DNS) {
        withContext(Dispatchers.IO) {
            InetAddress.getAllByName(c.host).joinToString { it.hostAddress ?: "" }
        }
    }

internal suspend fun tcpProbe(c: DiagnosticConfig) =
    runProbe(ProbeKind.TCP) {
        withContext(Dispatchers.IO) {
            Socket().use {
                it.connect(InetSocketAddress(c.host, c.port), c.timeout.inWholeMilliseconds.toInt())
            }
            "${c.host}:${c.port} connected"
        }
    }

internal suspend fun pingProbe(c: DiagnosticConfig): ProbeResult {
    val result =
        commandProbe(
            ProbeKind.PING,
            c.timeout,
            "ping",
            "-c",
            c.pingCount.toString(),
            c.host,
        )
    if (!result.success) return result
    val metrics = parsePingMetrics(result.detail) ?: return result
    return result.copy(detail = metrics.detail())
}

internal suspend fun traceProbe(c: DiagnosticConfig) =
    commandProbe(
        ProbeKind.TRACEROUTE,
        c.timeout * c.maxHops,
        "traceroute",
        "-m",
        c.maxHops.toString(),
        c.host,
    )

private suspend fun commandProbe(
    kind: ProbeKind,
    timeout: kotlin.time.Duration,
    vararg command: String,
) =
    runProbe(kind) {
        withTimeout(timeout) {
            withContext(Dispatchers.IO) {
                val process = ProcessBuilder(*command).redirectErrorStream(true).start()
                val job = coroutineContext[Job]
                val cancellationHandler = job?.invokeOnCompletion {
                    process.destroy()
                }
                try {
                    val output = process.inputStream.bufferedReader().use { it.readText() }
                    val exitCode = process.waitFor()
                    output.take(32_768).also {
                        if (exitCode != 0) error(it.ifBlank { "command failed" })
                    }
                } finally {
                    cancellationHandler?.dispose()
                    process.destroy()
                }
            }
        }
    }

internal fun httpDetail(url: String, status: Int) = "url=$url, status=$status"

private suspend fun fetch(kind: ProbeKind, url: String, timeoutMs: Int) =
    runProbe(kind) {
        withContext(Dispatchers.IO) {
            val c = URL(url).openConnection() as HttpURLConnection
            try {
                c.connectTimeout = timeoutMs
                c.readTimeout = timeoutMs
                c.instanceFollowRedirects = true
                c.connect()
                httpDetail(url, c.responseCode)
            } finally {
                c.disconnect()
            }
        }
    }

internal suspend fun httpProbe(c: DiagnosticConfig) =
    if (c.url.startsWith("https://")) httpsTimingProbe(c)
    else fetch(ProbeKind.HTTP, c.url, c.timeout.inWholeMilliseconds.toInt())

internal suspend fun externalIpProbe(c: DiagnosticConfig) =
    fetch(ProbeKind.EXTERNAL_IP, c.externalIpUrl, c.timeout.inWholeMilliseconds.toInt())

internal suspend fun networkProbe(context: Context) =
    runProbe(ProbeKind.NETWORK) {
        withContext(Dispatchers.IO) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: error("no active network")
            val caps = cm.getNetworkCapabilities(network) ?: error("network capabilities unavailable")
            val links = cm.getLinkProperties(network)
            val type =
                when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                    else -> "other"
                }
            networkDetail(
                type = type,
                validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                captivePortal = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL),
                vpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
                metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                addresses = links?.linkAddresses?.joinToString().orEmpty(),
                dns = links?.dnsServers?.joinToString().orEmpty(),
            )
        }
    }
