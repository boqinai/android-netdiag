package com.boqinai.android.netdiag

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.util.Locale
import java.util.TimeZone

internal data class SystemInfo(val device: DeviceInfo, val app: AppInfo) {
    companion object {
        val EMPTY =
            SystemInfo(
                DeviceInfo("", "", "", "", "", 0, emptyList(), 0, 0, "", "", "none"),
                AppInfo("", "", "", 0, false),
            )
    }
}

internal fun collectSystemInfoSafely(context: Context): SystemInfo =
    runCatching { collectSystemInfo(context) }.getOrDefault(SystemInfo.EMPTY)

private fun collectSystemInfo(context: Context): SystemInfo {
    val metrics = context.resources.displayMetrics
    val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = connectivity.activeNetwork?.let(connectivity::getNetworkCapabilities)
    val networkType =
        when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "wifi"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "cellular"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "ethernet"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "vpn"
            else -> "none"
        }
    val packageManager = context.packageManager
    val packageName = context.packageName
    val packageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, 0)
        }
    val versionCode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode
        else {
            @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
        }

    return SystemInfo(
        device =
            DeviceInfo(
                manufacturer = Build.MANUFACTURER.orEmpty(),
                brand = Build.BRAND.orEmpty(),
                model = Build.MODEL.orEmpty(),
                device = Build.DEVICE.orEmpty(),
                androidVersion = Build.VERSION.RELEASE.orEmpty(),
                apiLevel = Build.VERSION.SDK_INT,
                supportedAbis = Build.SUPPORTED_ABIS.toList(),
                screenWidthPixels = metrics.widthPixels,
                screenHeightPixels = metrics.heightPixels,
                locale = Locale.getDefault().toLanguageTag(),
                timeZone = TimeZone.getDefault().id,
                networkType = networkType,
            ),
        app =
            AppInfo(
                packageName = packageName,
                appName = packageManager.getApplicationLabel(context.applicationInfo).toString(),
                versionName = packageInfo.versionName.orEmpty(),
                versionCode = versionCode,
                debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
            ),
    )
}
