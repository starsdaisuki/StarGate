package com.stardaisuki.stargate.data

import java.util.UUID

/**
 * 网络配置文件
 * 保存一组完整的网络参数，可以一键切换
 */
data class NetworkProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,                    // 配置名称，如 "内网直连"、"旁路由代理"
    val icon: String = "router",         // 图标标识
    val color: Long = 0xFF6C9EFF,        // 卡片主题色

    // 网络配置
    val useDhcp: Boolean = false,        // 是否使用 DHCP
    val ipAddress: String = "",          // 静态 IP 地址
    val gateway: String = "",            // 网关
    val subnetMask: String = "255.255.255.0",  // 子网掩码（默认 /24）
    val dns1: String = "",               // 首选 DNS
    val dns2: String = "",               // 备用 DNS

    // 元数据
    val isActive: Boolean = false,       // 是否当前生效
    val lastUsed: Long = 0L,            // 上次使用时间戳
    val sortOrder: Int = 0               // 排序顺序
)

/**
 * 当前网络状态信息
 */
data class NetworkStatus(
    val isConnected: Boolean = false,
    val ssid: String = "",               // 当前 Wi-Fi 名称
    val ipAddress: String = "",
    val gateway: String = "",
    val dns1: String = "",
    val dns2: String = "",
    val subnetMask: String = "",
    val signalStrength: Int = 0,         // 信号强度 0-100
    val linkSpeed: Int = 0,              // 连接速度 Mbps
    val activeProfileId: String? = null  // 当前匹配的配置 ID
)
