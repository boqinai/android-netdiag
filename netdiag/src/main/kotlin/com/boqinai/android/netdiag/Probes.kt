package com.boqinai.android.netdiag

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

internal suspend fun runProbe(kind: ProbeKind, block: suspend () -> String): ProbeResult {
    val start = System.nanoTime()
    return try { ProbeResult(kind, true, (System.nanoTime()-start)/1_000_000, block()) }
    catch (e: CancellationException) { throw e }
    catch (e: Exception) { ProbeResult(kind, false, (System.nanoTime()-start)/1_000_000, "", e.message ?: e.javaClass.simpleName) }
}

internal suspend fun dnsProbe(c: DiagnosticConfig) = runProbe(ProbeKind.DNS) { withContext(Dispatchers.IO) { InetAddress.getAllByName(c.host).joinToString { it.hostAddress ?: "" } } }
internal suspend fun tcpProbe(c: DiagnosticConfig) = runProbe(ProbeKind.TCP) { withContext(Dispatchers.IO) { Socket().use { it.connect(InetSocketAddress(c.host,c.port),c.timeout.inWholeMilliseconds.toInt()) }; "${c.host}:${c.port} connected" } }
internal suspend fun pingProbe(c: DiagnosticConfig) = commandProbe(ProbeKind.PING, c.timeout.inWholeMilliseconds, "ping", "-c", c.pingCount.toString(), c.host)
internal suspend fun traceProbe(c: DiagnosticConfig) = commandProbe(ProbeKind.TRACEROUTE, c.timeout.inWholeMilliseconds*c.maxHops, "traceroute", "-m", c.maxHops.toString(), c.host)

private suspend fun commandProbe(kind: ProbeKind, timeoutMs: Long, vararg command: String) = runProbe(kind) {
    withTimeout(timeoutMs) { withContext(Dispatchers.IO) {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        try { while (true) try { process.exitValue(); break } catch (_: IllegalThreadStateException) { delay(50) }; process.inputStream.bufferedReader().use { it.readText().take(32_768) }.also { if (process.exitValue()!=0) error(it.ifBlank { "command failed" }) } }
        finally { process.destroy() }
    } }
}

private suspend fun fetch(kind: ProbeKind, url: String, timeoutMs: Int, body: Boolean) = runProbe(kind) {
    withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        try { c.connectTimeout=timeoutMs; c.readTimeout=timeoutMs; c.instanceFollowRedirects=true; c.connect(); if (body) c.inputStream.bufferedReader().use { it.readText().take(1024) } else "HTTP ${c.responseCode}" } finally { c.disconnect() }
    }
}
internal suspend fun httpProbe(c: DiagnosticConfig) = fetch(ProbeKind.HTTP,c.url,c.timeout.inWholeMilliseconds.toInt(),false)
internal suspend fun externalIpProbe(c: DiagnosticConfig) = fetch(ProbeKind.EXTERNAL_IP,c.externalIpUrl,c.timeout.inWholeMilliseconds.toInt(),true)
internal suspend fun networkProbe(context: Context) = runProbe(ProbeKind.NETWORK) {
    val cm=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network=cm.activeNetwork ?: error("no active network"); val caps=cm.getNetworkCapabilities(network); val links=cm.getLinkProperties(network)
    val type=when { caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)==true -> "wifi"; caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)==true -> "cellular"; else -> "other" }
    "type=$type, addresses=${links?.linkAddresses?.joinToString()}, dns=${links?.dnsServers?.joinToString()}"
}
