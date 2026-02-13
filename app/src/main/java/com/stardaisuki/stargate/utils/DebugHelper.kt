package com.stardaisuki.stargate.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address

/**
 * 调试工具 - 逐步检测各项功能是否正常
 */
object DebugHelper {

    /**
     * 运行全部诊断，返回每一步的结果
     */
    suspend fun runDiagnostics(context: Context): List<DiagnosticItem> {
        val results = mutableListOf<DiagnosticItem>()

        // 1. 检查 Root
        results.add(DiagnosticItem("Root 权限", "检查中..."))
        try {
            val rootResult = ShellExecutor.execRoot("id")
            if (rootResult.isSuccess && rootResult.stdout.contains("uid=0")) {
                results[results.lastIndex] = DiagnosticItem("Root 权限", "✓ 已获取", rootResult.stdout)
            } else {
                results[results.lastIndex] = DiagnosticItem("Root 权限", "✗ 未获取", "code=${rootResult.exitCode}, err=${rootResult.stderr}")
            }
        } catch (e: Exception) {
            results[results.lastIndex] = DiagnosticItem("Root 权限", "✗ 异常", e.message ?: "")
        }

        // 2. 检查 ConnectivityManager
        results.add(DiagnosticItem("ConnectivityManager", "检查中..."))
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            if (network != null) {
                val caps = cm.getNetworkCapabilities(network)
                val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                results[results.lastIndex] = DiagnosticItem(
                    "ConnectivityManager",
                    "✓ activeNetwork 存在",
                    "isWifi=$isWifi, network=$network"
                )
            } else {
                results[results.lastIndex] = DiagnosticItem("ConnectivityManager", "✗ activeNetwork = null")
            }
        } catch (e: Exception) {
            results[results.lastIndex] = DiagnosticItem("ConnectivityManager", "✗ 异常", e.message ?: "")
        }

        // 3. 检查 LinkProperties
        results.add(DiagnosticItem("LinkProperties", "检查中..."))
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            val lp = if (network != null) cm.getLinkProperties(network) else null
            if (lp != null) {
                val addrs = lp.linkAddresses.map { "${it.address.hostAddress}/${it.prefixLength}" }
                val routes = lp.routes.map { "dst=${it.destination}, gw=${it.gateway?.hostAddress}" }
                val dns = lp.dnsServers.map { it.hostAddress }
                val iface = lp.interfaceName

                results[results.lastIndex] = DiagnosticItem(
                    "LinkProperties",
                    "✓ 获取成功",
                    "iface=$iface\naddrs=$addrs\nroutes=$routes\ndns=$dns"
                )
            } else {
                results[results.lastIndex] = DiagnosticItem("LinkProperties", "✗ null")
            }
        } catch (e: Exception) {
            results[results.lastIndex] = DiagnosticItem("LinkProperties", "✗ 异常", e.message ?: "")
        }

        // 4. 检查 WifiManager
        results.add(DiagnosticItem("WifiManager", "检查中..."))
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wm.connectionInfo
            results[results.lastIndex] = DiagnosticItem(
                "WifiManager",
                "✓ 获取成功",
                "ssid=${info?.ssid}, rssi=${info?.rssi}, linkSpeed=${info?.linkSpeed}Mbps"
            )
        } catch (e: Exception) {
            results[results.lastIndex] = DiagnosticItem("WifiManager", "✗ 异常", e.message ?: "")
        }

        // 5. Shell 命令测试
        results.add(DiagnosticItem("Shell: ip route", "检查中..."))
        try {
            val ipRoute = ShellExecutor.exec("ip route")
            results[results.lastIndex] = DiagnosticItem(
                "Shell: ip route",
                if (ipRoute.isSuccess) "✓ 成功" else "✗ 失败",
                ipRoute.stdout.ifEmpty { ipRoute.stderr }
            )
        } catch (e: Exception) {
            results[results.lastIndex] = DiagnosticItem("Shell: ip route", "✗ 异常", e.message ?: "")
        }

        // 6. Shell: ip addr
        results.add(DiagnosticItem("Shell: ip addr", "检查中..."))
        try {
            val ipAddr = ShellExecutor.exec("ip -4 addr show")
            results[results.lastIndex] = DiagnosticItem(
                "Shell: ip addr",
                if (ipAddr.isSuccess) "✓ 成功" else "✗ 失败",
                ipAddr.stdout.take(500).ifEmpty { ipAddr.stderr }
            )
        } catch (e: Exception) {
            results[results.lastIndex] = DiagnosticItem("Shell: ip addr", "✗ 异常", e.message ?: "")
        }

        // 7. Root Shell 测试
        results.add(DiagnosticItem("Root Shell: ip route", "检查中..."))
        try {
            val rootIpRoute = ShellExecutor.execRoot("ip route show")
            results[results.lastIndex] = DiagnosticItem(
                "Root Shell: ip route",
                if (rootIpRoute.isSuccess) "✓ 成功" else "✗ 失败",
                rootIpRoute.stdout.ifEmpty { rootIpRoute.stderr }
            )
        } catch (e: Exception) {
            results[results.lastIndex] = DiagnosticItem("Root Shell: ip route", "✗ 异常", e.message ?: "")
        }

        // 8. DNS 检查
        results.add(DiagnosticItem("DNS 配置", "检查中..."))
        try {
            val dns1 = ShellExecutor.exec("getprop net.dns1")
            val dns2 = ShellExecutor.exec("getprop net.dns2")
            results[results.lastIndex] = DiagnosticItem(
                "DNS 配置",
                "✓",
                "dns1=${dns1.stdout}\ndns2=${dns2.stdout}"
            )
        } catch (e: Exception) {
            results[results.lastIndex] = DiagnosticItem("DNS 配置", "✗ 异常", e.message ?: "")
        }

        return results
    }

    data class DiagnosticItem(
        val title: String,
        val status: String,
        val detail: String = ""
    )
}
