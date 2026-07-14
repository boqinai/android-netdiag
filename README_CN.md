# Android NetDiag

[English](README.md)

一个协程优先的 Android 网络诊断 SDK（`minSdk 23`），可将网络状态、DNS、Ping、TCP、IPv4/IPv6、Traceroute、HTTP(S) 和外网 IP 检测结果汇总为结构化报告。报告还会自动包含设备、系统和当前应用的基础信息。

## 使用方式

```kotlin
val report = NetDiag(context).run(DiagnosticConfig("baidu.com"))
println(report.toJson())

NetDiag(context).events(DiagnosticConfig("baidu.com")).collect { event ->
    // 更新诊断进度界面
}
```

将 `netdiag` 模块添加为项目依赖，并声明 `INTERNET` 和 `ACCESS_NETWORK_STATE` 权限。取消收集协程即可取消诊断任务。

Ping 和 Traceroute 依赖 Android 系统镜像提供的命令。如果设备不支持对应命令，SDK 会将其记录为失败的探测结果，但不会中断后续诊断。

Ping 结果包含最小、平均、最大 RTT、抖动和丢包率。HTTPS 结果包含 DNS、TCP、TLS、首字节和总耗时。网络状态还会报告系统验证、登录页、VPN 和计费网络标志；IPv4 与 IPv6 分别验证到目标端口的连接能力。

## 构建并运行 Demo

```powershell
.\gradlew.bat :demo:assembleDebug
adb install -r demo\build\outputs\apk\debug\demo-debug.apk
```
## 基础信息

`DiagnosticReport` 的 `device` 和 `app` 字段包含：

- 设备：厂商、品牌、型号、设备名、Android 版本/API、CPU ABI、屏幕分辨率、语言、时区和网络类型。
- 应用：应用名、包名、版本名、版本号和 Debug/Release 状态。

这些信息不需要新增权限。SDK 不采集 IMEI、Android ID、手机号、MAC、序列号等设备唯一标识或隐私信息。