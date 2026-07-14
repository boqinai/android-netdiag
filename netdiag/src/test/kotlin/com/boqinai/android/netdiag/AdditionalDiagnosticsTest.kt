package com.boqinai.android.netdiag

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Test

class AdditionalDiagnosticsTest {
    @Test
    fun separatesIpv4AndIpv6Addresses() {
        val addresses =
            listOf(InetAddress.getByName("127.0.0.1"), InetAddress.getByName("::1"))

        assertEquals(listOf("127.0.0.1"), filterAddresses(addresses, ipv6 = false).map { it.hostAddress })
        assertEquals(listOf("0:0:0:0:0:0:0:1"), filterAddresses(addresses, ipv6 = true).map { it.hostAddress })
    }

    @Test
    fun formatsHttpsPhaseMetrics() {
        assertEquals(
            "dnsMs=1, tcpMs=2, tlsMs=3, ttfbMs=4, totalMs=10, status=200",
            HttpsMetrics(1, 2, 3, 4, 10, 200).detail(),
        )
    }

    @Test
    fun reportsNetworkRestrictions() {
        assertEquals(
            "type=wifi, validated=false, captivePortal=true, vpn=true, metered=true, " +
                "addresses=10.0.0.2, dns=10.0.0.1",
            networkDetail(
                type = "wifi",
                validated = false,
                captivePortal = true,
                vpn = true,
                metered = true,
                addresses = "10.0.0.2",
                dns = "10.0.0.1",
            ),
        )
    }
}
