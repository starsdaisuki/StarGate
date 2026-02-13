package com.stardaisuki.stargate.utils

import android.content.Context
import android.util.Log
import com.stardaisuki.stargate.data.NetworkProfile
import com.stardaisuki.stargate.data.NetworkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 网络管理器
 *
 * 核心原理：Android 给每个网络接口分配了专属路由表
 * wlan0 的路由表名就是 "wlan0"
 * 切换网关只需：ip route replace default via <网关> dev wlan0 table wlan0
 */
class NetworkManager(private val context: Context) {

    private val TAG = "StarGate.Network"

    /**
     * 获取当前网络状态
     */
    suspend fun getNetworkStatus(): NetworkStatus = withContext(Dispatchers.IO) {
        try {
            val iface = getWifiInterface()

            // IP
            val ipResult = ShellExecutor.execRoot(
                "ip -4 addr show $iface 2>/dev/null | grep 'inet ' | head -1 | awk '{print \$2}'"
            )
            val ipWithPrefix = ipResult.stdout.trim()
            val ipAddress = ipWithPrefix.split("/").firstOrNull() ?: ""
            val prefix = ipWithPrefix.split("/").getOrNull(1)?.toIntOrNull() ?: 24

            if (ipAddress.isEmpty()) {
                return@withContext NetworkStatus(isConnected = false)
            }

            // 网关：从 wlan0 专属路由表读取（这才是真正生效的网关）
            val gwResult = ShellExecutor.execRoot(
                "ip route show table $iface 2>/dev/null | grep 'default' | head -1 | awk '{print \$3}'"
            )
            val gateway = gwResult.stdout.trim()

            // DNS
            val dns1 = ShellExecutor.execRoot("getprop net.dns1").stdout.trim()
            val dns2 = ShellExecutor.execRoot("getprop net.dns2").stdout.trim()

            // SSID
            val ssid = getSsid()

            // 连接速度
            val speedResult = ShellExecutor.execRoot(
                "iw dev $iface link 2>/dev/null | grep 'tx bitrate' | awk '{print \$3}' | cut -d. -f1"
            )
            val linkSpeed = speedResult.stdout.trim().toIntOrNull() ?: 0

            NetworkStatus(
                isConnected = true,
                ssid = ssid,
                ipAddress = ipAddress,
                gateway = gateway,
                dns1 = dns1,
                dns2 = dns2,
                subnetMask = prefixToSubnetMask(prefix),
                linkSpeed = linkSpeed
            )
        } catch (e: Exception) {
            Log.e(TAG, "getNetworkStatus error: ${e.message}", e)
            NetworkStatus(isConnected = false)
        }
    }

    /**
     * 应用网络配置
     *
     * 关键：改的是 wlan0 专属路由表，不是 main 表！
     */
    suspend fun applyProfile(profile: NetworkProfile): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!ShellExecutor.checkRoot()) {
                return@withContext Result.failure(Exception("没有 Root 权限"))
            }

            val iface = getWifiInterface()
            Log.d(TAG, "Applying '${profile.name}': gateway=${profile.gateway}")

            val commands = mutableListOf<String>()

            // 核心：修改 wlan0 专属路由表的默认网关
            commands.add("ip route replace default via ${profile.gateway} dev $iface table $iface")

            // DNS
            if (profile.dns1.isNotBlank()) {
                commands.add("setprop net.dns1 ${profile.dns1}")
                // iptables 强制 DNS 重定向
                commands.add("iptables -t nat -F stargate_dns 2>/dev/null || true")
                commands.add("iptables -t nat -D OUTPUT -j stargate_dns 2>/dev/null || true")
                commands.add("iptables -t nat -N stargate_dns 2>/dev/null || true")
                commands.add("iptables -t nat -A stargate_dns -p udp --dport 53 -j DNAT --to-destination ${profile.dns1}:53")
                commands.add("iptables -t nat -A stargate_dns -p tcp --dport 53 -j DNAT --to-destination ${profile.dns1}:53")
                commands.add("iptables -t nat -A OUTPUT -j stargate_dns")
            }
            if (profile.dns2.isNotBlank()) {
                commands.add("setprop net.dns2 ${profile.dns2}")
            } else {
                commands.add("setprop net.dns2 ''")
            }

            Log.d(TAG, "Commands: $commands")
            val result = ShellExecutor.execRootCommands(commands)
            Log.d(TAG, "Result: code=${result.exitCode}, stdout=${result.stdout}, stderr=${result.stderr}")

            delay(300)

            // 验证
            val pingResult = ShellExecutor.execRoot("ping -c 1 -W 2 ${profile.gateway}")
            return@withContext if (pingResult.isSuccess) {
                Result.success("✓ 已切换到「${profile.name}」")
            } else {
                Result.success("已切换到「${profile.name}」（网关未响应 ping）")
            }
        } catch (e: Exception) {
            Log.e(TAG, "applyProfile error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun pingTest(host: String): Result<String> = withContext(Dispatchers.IO) {
        val result = ShellExecutor.exec("ping -c 3 -W 2 $host")
        if (result.isSuccess) {
            val regex = "avg.*?([\\d.]+)".toRegex()
            val avg = regex.find(result.stdout)?.groupValues?.get(1) ?: "?"
            Result.success("${avg}ms")
        } else {
            Result.failure(Exception("不可达"))
        }
    }

    private suspend fun getWifiInterface(): String {
        val result = ShellExecutor.execRoot("ip route 2>/dev/null | grep 'default' | head -1 | awk '{print \$5}'")
        if (result.stdout.trim().isNotEmpty()) return result.stdout.trim()
        val result2 = ShellExecutor.execRoot("ls /sys/class/net/ | grep -E '^wlan'")
        if (result2.stdout.trim().isNotEmpty()) return result2.stdout.trim().lines().first()
        return "wlan0"
    }

    private suspend fun getSsid(): String {
        val result = ShellExecutor.execRoot("dumpsys wifi 2>/dev/null | grep 'mWifiInfo' | head -1")
        if (result.isSuccess && result.stdout.contains("SSID:")) {
            val regex = "SSID: \"?([^\",$]+)".toRegex()
            val match = regex.find(result.stdout)
            if (match != null) return match.groupValues[1].removeSurrounding("\"")
        }
        return "Wi-Fi"
    }

    private fun prefixToSubnetMask(prefix: Int): String {
        val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
        return "${(mask shr 24) and 0xFF}.${(mask shr 16) and 0xFF}.${(mask shr 8) and 0xFF}.${mask and 0xFF}"
    }
}