# Android NetDiag

[English](README.md)

一个协程优先的 Android 网络诊断 SDK（`minSdk 23`），可将网络状态、DNS、Ping、TCP 连接、Traceroute、HTTP(S) 和外网 IP 检测结果汇总为结构化报告。

## 使用方式

```kotlin
val report = NetDiag(context).run(DiagnosticConfig("example.com"))
println(report.toJson())

NetDiag(context).events(DiagnosticConfig("example.com")).collect { event ->
    // 更新诊断进度界面
}
```

将 `netdiag` 模块添加为项目依赖，并声明 `INTERNET` 和 `ACCESS_NETWORK_STATE` 权限。取消收集协程即可取消诊断任务。

Ping 和 Traceroute 依赖 Android 系统镜像提供的命令。如果设备不支持对应命令，SDK 会将其记录为失败的探测结果，但不会中断后续诊断。

## 构建并运行 Demo

```powershell
.\gradlew.bat :demo:assembleDebug
adb install -r demo\build\outputs\apk\debug\demo-debug.apk
```