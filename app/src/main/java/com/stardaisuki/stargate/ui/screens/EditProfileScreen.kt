package com.stardaisuki.stargate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stardaisuki.stargate.MainViewModel
import com.stardaisuki.stargate.data.NetworkProfile
import com.stardaisuki.stargate.ui.theme.*

private val colorOptions = listOf(
    0xFF6C9EFF, 0xFF3FB950, 0xFFD29922, 0xFF9D7AFF,
    0xFF64FFDA, 0xFFFF8C42, 0xFFF85149, 0xFFFF6EB4,
)

private val iconOptions = listOf(
    "home" to Icons.Rounded.Home,
    "vpn" to Icons.Rounded.VpnKey,
    "router" to Icons.Rounded.Router,
    "public" to Icons.Rounded.Public,
    "lan" to Icons.Rounded.Lan,
    "speed" to Icons.Rounded.Speed,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profileId: String?,
    viewModel: MainViewModel,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val isNew = profileId == null

    var name by remember { mutableStateOf("") }
    var gateway by remember { mutableStateOf("") }
    var dns1 by remember { mutableStateOf("") }
    var dns2 by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(0xFF6C9EFF) }
    var selectedIcon by remember { mutableStateOf("router") }

    // 编辑模式加载数据
    LaunchedEffect(profileId) {
        if (profileId != null) {
            val profile = viewModel.getProfile(profileId)
            if (profile != null) {
                name = profile.name
                gateway = profile.gateway
                dns1 = profile.dns1
                dns2 = profile.dns2
                selectedColor = profile.color
                selectedIcon = profile.icon
            }
        }
    }

    val ipRegex = remember { Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$") }
    val isGatewayValid = gateway.isEmpty() || ipRegex.matches(gateway)
    val canSave = name.isNotBlank() && gateway.isNotBlank() && isGatewayValid

    fun handleSave() {
        val profile = NetworkProfile(
            id = profileId ?: java.util.UUID.randomUUID().toString(),
            name = name.trim(),
            icon = selectedIcon,
            color = selectedColor,
            useDhcp = false,
            ipAddress = "",
            gateway = gateway.trim(),
            subnetMask = "255.255.255.0",
            dns1 = dns1.trim().ifBlank { gateway.trim() },  // DNS 默认和网关一样
            dns2 = dns2.trim()
        )
        if (isNew) viewModel.addProfile(profile)
        else viewModel.updateProfile(profile)
        onSave()
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isNew) "新建配置" else "编辑配置", color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "返回", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { handleSave() },
                        enabled = canSave
                    ) {
                        Text("保存", color = if (canSave) StarBlue else TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 基本信息 ===
            SectionTitle("基本信息")

            StyledTextField(
                value = name,
                onValueChange = { name = it },
                label = "配置名称",
                placeholder = "例如：旁路由代理"
            )

            // 颜色选择
            Text("主题色", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                colorOptions.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .then(
                                if (color == selectedColor)
                                    Modifier.border(2.dp, TextPrimary, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedColor = color }
                    ) {
                        if (color == selectedColor) {
                            Icon(
                                Icons.Rounded.Check, null,
                                modifier = Modifier.align(Alignment.Center).size(18.dp),
                                tint = DarkBg
                            )
                        }
                    }
                }
            }

            // 图标选择
            Text("图标", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                iconOptions.forEach { (key, icon) ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (key == selectedIcon) Color(selectedColor).copy(alpha = 0.2f)
                                else DarkCard
                            )
                            .border(
                                1.dp,
                                if (key == selectedIcon) Color(selectedColor).copy(alpha = 0.5f)
                                else DarkCardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedIcon = key },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon, null,
                            tint = if (key == selectedIcon) Color(selectedColor) else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DarkCardBorder)

            // === 网络配置 ===
            SectionTitle("网络配置")

            // 说明文字
            Card(
                colors = CardDefaults.cardColors(containerColor = StarBlue.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "只需填写网关和 DNS，IP 地址保持不变。\n切换时仅修改流量出口和域名解析。",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = StarBlue.copy(alpha = 0.8f)
                )
            }

            StyledTextField(
                value = gateway,
                onValueChange = { gateway = it },
                label = "网关地址",
                placeholder = "192.168.50.1",
                keyboardType = KeyboardType.Number,
                isError = gateway.isNotEmpty() && !isGatewayValid,
                errorText = "格式不正确"
            )

            StyledTextField(
                value = dns1,
                onValueChange = { dns1 = it },
                label = "首选 DNS（留空则和网关相同）",
                placeholder = "192.168.50.1",
                keyboardType = KeyboardType.Number
            )

            StyledTextField(
                value = dns2,
                onValueChange = { dns2 = it },
                label = "备用 DNS（可选）",
                placeholder = "223.5.5.5",
                keyboardType = KeyboardType.Number
            )

            // 快捷模板
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DarkCardBorder)
            SectionTitle("快捷模板")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickTemplateChip(
                    label = "🏠 内网直连",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        name = "内网直连"
                        gateway = "192.168.50.1"
                        dns1 = "192.168.50.1"
                        dns2 = ""
                        selectedColor = 0xFF3FB950
                        selectedIcon = "home"
                    }
                )
                QuickTemplateChip(
                    label = "🌐 旁路由代理",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        name = "旁路由代理"
                        gateway = "192.168.50.3"
                        dns1 = "192.168.50.3"
                        dns2 = ""
                        selectedColor = 0xFF6C9EFF
                        selectedIcon = "vpn"
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = StarBlue)
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String = ""
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) StatusRed else StarBlue,
                unfocusedBorderColor = if (isError) StatusRed.copy(alpha = 0.5f) else DarkCardBorder,
                focusedLabelColor = if (isError) StatusRed else StarBlue,
                unfocusedLabelColor = TextSecondary,
                cursorColor = StarBlue,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard
            )
        )
        if (isError && errorText.isNotEmpty()) {
            Text(
                errorText, color = StatusRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun QuickTemplateChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkCardBorder)
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
