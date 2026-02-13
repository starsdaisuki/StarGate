package com.stardaisuki.stargate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stardaisuki.stargate.data.NetworkStatus
import com.stardaisuki.stargate.ui.theme.*

/**
 * 网络状态卡片 - 显示当前连接信息
 */
@Composable
fun StatusCard(
    status: NetworkStatus,
    activeProfileName: String?,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = if (status.isConnected) StatusGreen else StatusRed,
        label = "statusColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DarkCard,
                            DarkCard.copy(alpha = 0.8f),
                            md_dark_primaryContainer.copy(alpha = 0.3f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            DarkCardBorder,
                            StarBlue.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                // 顶部：连接状态
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 状态指示灯
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (status.isConnected)
                                Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (status.isConnected) "已连接" else "未连接",
                            style = MaterialTheme.typography.titleMedium,
                            color = statusColor
                        )
                        if (status.ssid.isNotEmpty()) {
                            Text(
                                text = status.ssid,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 当前生效的配置
                    if (activeProfileName != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StarBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = activeProfileName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = StarBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // 网络详情
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem(label = "IP 地址", value = status.ipAddress.ifEmpty { "--" })
                    InfoItem(label = "网关", value = status.gateway.ifEmpty { "--" })
                    InfoItem(label = "DNS", value = status.dns1.ifEmpty { "--" })
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = TextPrimary
        )
    }
}
