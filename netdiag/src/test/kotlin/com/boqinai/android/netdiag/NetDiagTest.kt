package com.boqinai.android.netdiag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class NetDiagTest {
 @Test fun ordersAndIsolatesResults()= runBlocking { val events=NetDiag { kind,_ -> if(kind==ProbeKind.DNS) ProbeResult(kind,false,0,"","bad") else ProbeResult(kind,true,0,kind.name) }.events(DiagnosticConfig("x")).toList(); assertEquals(15,events.size); assertEquals(ProbeKind.NETWORK,(events[0] as DiagnosticEvent.Started).kind); assertEquals(7,(events.last() as DiagnosticEvent.Finished).report.results.size) }
 @Test fun cancellationPropagates() { assertThrows(CancellationException::class.java) { runBlocking { NetDiag { _,_-> throw CancellationException() }.run(DiagnosticConfig("x")) } } }
}
