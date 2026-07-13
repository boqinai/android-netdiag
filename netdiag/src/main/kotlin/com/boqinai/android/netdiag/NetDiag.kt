package com.boqinai.android.netdiag

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last

class NetDiag
private constructor(
    private val probes: List<Pair<ProbeKind, suspend (DiagnosticConfig) -> ProbeResult>>
) {
    constructor(
        context: Context
    ) : this(
        listOf(
            ProbeKind.NETWORK to { networkProbe(context) },
            ProbeKind.DNS to ::dnsProbe,
            ProbeKind.PING to ::pingProbe,
            ProbeKind.TCP to ::tcpProbe,
            ProbeKind.TRACEROUTE to ::traceProbe,
            ProbeKind.HTTP to ::httpProbe,
            ProbeKind.EXTERNAL_IP to ::externalIpProbe,
        )
    )

    internal constructor(
        probe: suspend (ProbeKind, DiagnosticConfig) -> ProbeResult
    ) : this(ProbeKind.entries.map { it to { c -> probe(it, c) } })

    fun events(config: DiagnosticConfig): Flow<DiagnosticEvent> = flow {
        val started = System.currentTimeMillis()
        val results = mutableListOf<ProbeResult>()
        for ((kind, probe) in probes) {
            emit(DiagnosticEvent.Started(kind))
            probe(config).also {
                results += it
                emit(DiagnosticEvent.Completed(it))
            }
        }
        emit(
            DiagnosticEvent.Finished(
                DiagnosticReport(started, System.currentTimeMillis() - started, results)
            )
        )
    }

    suspend fun run(config: DiagnosticConfig): DiagnosticReport =
        (events(config).last() as DiagnosticEvent.Finished).report
}
