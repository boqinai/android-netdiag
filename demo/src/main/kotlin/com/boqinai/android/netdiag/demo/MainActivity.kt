package com.boqinai.android.netdiag.demo

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.boqinai.android.netdiag.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var job: Job?=null
    override fun onCreate(state: Bundle?) { super.onCreate(state); setContentView(R.layout.activity_main)
        val host=findViewById<EditText>(R.id.host); val port=findViewById<EditText>(R.id.port); val url=findViewById<EditText>(R.id.url); val ip=findViewById<EditText>(R.id.ipUrl); val output=findViewById<TextView>(R.id.output)
        findViewById<Button>(R.id.run).setOnClickListener { try { val config=DiagnosticConfig(host.text.toString(),port.text.toString().toInt(),url.text.toString(),externalIpUrl=ip.text.toString()); output.text=""; job?.cancel(); job=lifecycleScope.launch { NetDiag(this@MainActivity).events(config).collect { e -> output.append(when(e) { is DiagnosticEvent.Started -> "\n▶ ${e.kind}\n"; is DiagnosticEvent.Completed -> "${if(e.result.success) "✓" else "✗"} ${e.result.detail.ifBlank { e.result.error.orEmpty() }}\n"; is DiagnosticEvent.Finished -> e.report.toJson()+"\n" }) } } } catch(e:Exception) { output.text=e.message } }
        findViewById<Button>(R.id.cancel).setOnClickListener { job?.cancel(); output.append("\nCancelled\n") }
    }
}
