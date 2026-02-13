package com.stardaisuki.stargate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stardaisuki.stargate.data.NetworkProfile
import com.stardaisuki.stargate.ui.theme.*

/**
 * 获取配置对应的图标
 */
fun getProfileIcon(icon: String): ImageVector {
    return when (icon) {
        "router" -> Icons.Rounded.Router
        "vpn" -> Icons.Rounded.VpnKey
        "home" -> Icons.Rounded.Home
        "public" -> Icons.Rounded.Public
        "lan" -> Icons.Rounded.Lan
        "speed" -> Icons.Rounded.Speed
        else -> Icons.Rounded.Wifi
    }
}

/**
 * 网络配置卡片
 */
@Composable
fun ProfileCard(
    profile: NetworkProfile,
    onSwitch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profileColor = Color(profile.color)
    val borderColor by animateColorAsState(
        targetValue = if (profile.isActive) profileColor.copy(alpha = 0.6f) else DarkCardBorder,
        animationSpec = tween(300),
        label = "borderColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (profile.isActive) 1f else 0.98f,
        animationSpec = tween(200),
        label = "scale"
    )

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (profile.isActive) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    profileColor.copy(alpha = 0.08f),
                                    DarkCard
                                )
                            )
                        )
                    } else Modifier
                )
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onSwitch() }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 左侧图标
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(profileColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getProfileIcon(profile.icon),
                        contentDescription = null,
                        tint = profileColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // 中间信息
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (profile.isActive) profileColor else TextPrimary
                        )
                        if (profile.isActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = profileColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "生效中",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = profileColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 网络参数摘要
                    Text(
                        text = if (profile.useDhcp) {
                            "DHCP 自动获取"
                        } else {
                            "${profile.gateway}  •  ${profile.dns1}"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        color = TextSecondary
                    )
                }

                // 右侧操作
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "更多",
                            tint = TextSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = DarkSurface
                    ) {
                        DropdownMenuItem(
                            text = { Text("编辑", color = TextPrimary) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Edit, null, tint = TextSecondary)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = StatusRed) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Delete, null, tint = StatusRed)
                            }
                        )
                    }
                }
            }
        }
    }
}
