package com.boqinai.android.netdiag

import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun filterAddresses(addresses: List<InetAddress>, ipv6: Boolean): List<InetAddress> =
    addresses.filter { if (ipv6) it is Inet6Address else it is Inet4Address }

internal suspend fun ipFamilyProbe(c: DiagnosticConfig, ipv6: Boolean): ProbeResult {
    val kind = if (ipv6) ProbeKind.IPV6 else ProbeKind.IPV4
    return runProbe(kind) {
        withContext(Dispatchers.IO) {
            val addresses = filterAddresses(InetAddress.getAllByName(c.host).toList(), ipv6)
            check(addresses.isNotEmpty()) { "no ${if (ipv6) "IPv6" else "IPv4"} address" }
            var lastError: Exception? = null
            for (address in addresses) {
                try {
                    val started = System.nanoTime()
                    Socket().use {
                        it.connect(InetSocketAddress(address, c.port), c.timeout.inWholeMilliseconds.toInt())
                    }
                    return@withContext "address=${address.hostAddress}, connectMs=${elapsedMs(started)}"
                } catch (e: Exception) {
                    lastError = e
                }
            }
            throw lastError ?: IllegalStateException("connection failed")
        }
    }
}

internal data class HttpsMetrics(
    val dnsMs: Long,
    val tcpMs: Long,
    val tlsMs: Long,
    val ttfbMs: Long,
    val totalMs: Long,
    val status: Int,
) {
    fun detail(): String =
        "dnsMs=$dnsMs, tcpMs=$tcpMs, tlsMs=$tlsMs, ttfbMs=$ttfbMs, totalMs=$totalMs, status=$status"
}

internal suspend fun httpsTimingProbe(c: DiagnosticConfig): ProbeResult =
    runProbe(ProbeKind.HTTP) {
        withContext(Dispatchers.IO) {
            val url = URL(c.url)
            require(url.protocol == "https") { "HTTPS timing requires an https URL" }
            val totalStart = System.nanoTime()
            val dnsStart = System.nanoTime()
            val address = InetAddress.getAllByName(url.host).first()
            val dnsMs = elapsedMs(dnsStart)
            val port = if (url.port > 0) url.port else 443
            val socket = Socket()
            try {
                val tcpStart = System.nanoTime()
                socket.connect(InetSocketAddress(address, port), c.timeout.inWholeMilliseconds.toInt())
                val tcpMs = elapsedMs(tcpStart)
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val ssl = factory.createSocket(socket, url.host, port, true) as SSLSocket
                ssl.soTimeout = c.timeout.inWholeMilliseconds.toInt()
                val tlsStart = System.nanoTime()
                ssl.startHandshake()
                check(HttpsURLConnection.getDefaultHostnameVerifier().verify(url.host, ssl.session)) {
                    "TLS hostname verification failed"
                }
                val tlsMs = elapsedMs(tlsStart)
                val path = (url.path.ifEmpty { "/" }) + (url.query?.let { "?$it" } ?: "")
                val writer = OutputStreamWriter(ssl.outputStream, Charsets.US_ASCII)
                val ttfbStart = System.nanoTime()
                writer.write("GET $path HTTP/1.1\r\nHost: ${url.host}\r\nConnection: close\r\n\r\n")
                writer.flush()
                val statusLine = ssl.inputStream.bufferedReader().readLine().orEmpty()
                val status = statusLine.split(' ').getOrNull(1)?.toIntOrNull() ?: 0
                HttpsMetrics(dnsMs, tcpMs, tlsMs, elapsedMs(ttfbStart), elapsedMs(totalStart), status).detail()
            } finally {
                socket.close()
            }
        }
    }

internal fun networkDetail(
    type: String,
    validated: Boolean,
    captivePortal: Boolean,
    vpn: Boolean,
    metered: Boolean,
    addresses: String,
    dns: String,
): String =
    "type=$type, validated=$validated, captivePortal=$captivePortal, vpn=$vpn, " +
        "metered=$metered, addresses=$addresses, dns=$dns"

private fun elapsedMs(started: Long) = (System.nanoTime() - started) / 1_000_000
