package com.boqinai.android.netdiag.demo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.boqinai.android.netdiag.DiagnosticConfig
import com.boqinai.android.netdiag.DiagnosticEvent
import com.boqinai.android.netdiag.NetDiag
import com.boqinai.android.netdiag.ProbeKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var job: Job? = null

    private val labels =
        mapOf(
            ProbeKind.NETWORK to "当前网络",
            ProbeKind.DNS to "DNS 解析",
            ProbeKind.PING to "网络延迟",
            ProbeKind.TCP to "互联网连接",
            ProbeKind.TRACEROUTE to "路由信息",
            ProbeKind.HTTP to "网页访问",
            ProbeKind.EXTERNAL_IP to "业务服务",
        )

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val summary = findViewById<TextView>(R.id.summary)
        val output = findViewById<TextView>(R.id.output)
        val details = findViewById<Button>(R.id.details)
        val run = findViewById<Button>(R.id.run)
        val statusViews =
            mapOf(
                ProbeKind.NETWORK to findViewById<TextView>(R.id.statusNetwork),
                ProbeKind.DNS to findViewById(R.id.statusDns),
                ProbeKind.PING to findViewById(R.id.statusPing),
                ProbeKind.TCP to findViewById(R.id.statusTcp),
                ProbeKind.TRACEROUTE to findViewById(R.id.statusTrace),
                ProbeKind.HTTP to findViewById(R.id.statusHttp),
                ProbeKind.EXTERNAL_IP to findViewById(R.id.statusService),
            )

        details.setOnClickListener {
            val showing = output.isVisible
            output.isVisible = !showing
            details.text = if (showing) "查看详细结果" else "收起详细结果"
        }

        run.setOnClickListener {
            if (job?.isActive == true) {
                job?.cancel()
                return@setOnClickListener
            }

            statusViews.forEach { (kind, view) -> setStatus(view, kind, "等待检测") }
            summary.text = "正在检测，请稍候…"
            output.text = ""
            output.isVisible = false
            details.isVisible = false
            run.text = "停止检测"

            val config =
                DiagnosticConfig(
                    host = "baidu.com",
                    url = "https://www.baidu.com",
                    externalIpUrl = "https://www.boqinai.com",
                )

            job = lifecycleScope.launch {
                var abnormal = 0
                var slow = 0
                try {
                    NetDiag(this@MainActivity).events(config).collect { event ->
                        when (event) {
                            is DiagnosticEvent.Started ->
                                setStatus(statusViews.getValue(event.kind), event.kind, "检测中…")

                            is DiagnosticEvent.Completed -> {
                                val result = event.result
                                val level = assess(result).level
                                if (level == AssessmentLevel.ABNORMAL) abnormal++
                                if (level == AssessmentLevel.SLOW) slow++
                                setStatus(
                                    statusViews.getValue(result.kind),
                                    result.kind,
                                    displayDurationMs(level, result.durationMs)?.let { durationMs ->
                                        getString(
                                            R.string.status_with_duration,
                                            level.label,
                                            durationMs,
                                        )
                                    } ?: level.label,
                                    level,
                                )
                            }

                            is DiagnosticEvent.Finished -> {
                                summary.text = summaryText(abnormal, slow, event.report.durationMs)
                                summary.setTextColor(
                                    color(
                                        when {
                                            abnormal > 0 -> AssessmentLevel.ABNORMAL
                                            slow > 0 -> AssessmentLevel.SLOW
                                            else -> AssessmentLevel.NORMAL
                                        }
                                    )
                                )
                                output.text = event.report.toJson()
                                details.isVisible = true
                            }
                        }
                    }
                } finally {
                    run.text = "开始检测"
                    if (summary.text.toString().startsWith("正在检测")) summary.text = "检测已停止"
                }
            }
        }
    }

    private fun setStatus(
        view: TextView,
        kind: ProbeKind,
        status: String,
        level: AssessmentLevel? = null,
    ) {
        view.text = getString(R.string.status_format, labels.getValue(kind), status)
        view.setTextColor(level?.let(::color) ?: "#666666".toColorInt())
    }

    private fun summaryText(abnormal: Int, slow: Int, durationMs: Long): String =
        when {
            abnormal > 0 && slow > 0 ->
                getString(R.string.summary_abnormal_slow, abnormal, slow, durationMs)
            abnormal > 0 -> getString(R.string.summary_abnormal, abnormal, durationMs)
            slow > 0 -> getString(R.string.summary_slow, slow, durationMs)
            else -> getString(R.string.summary_normal, durationMs)
        }

    private fun color(level: AssessmentLevel): Int =
        when (level) {
            AssessmentLevel.NORMAL -> "#07C160"
            AssessmentLevel.SLOW -> "#FA9D3B"
            AssessmentLevel.ABNORMAL -> "#E64340"
            AssessmentLevel.UNSUPPORTED -> "#999999"
        }.toColorInt()
}
