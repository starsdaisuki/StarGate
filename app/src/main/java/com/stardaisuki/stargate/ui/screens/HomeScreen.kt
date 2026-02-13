package com.stardaisuki.stargate.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stardaisuki.stargate.MainViewModel
import com.stardaisuki.stargate.data.NetworkProfile
import com.stardaisuki.stargate.ui.components.ProfileCard
import com.stardaisuki.stargate.ui.components.StatusCard
import com.stardaisuki.stargate.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToEdit: (profileId: String?) -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val isSwitching by viewModel.isSwitching.collectAsState()
    val hasRoot by viewModel.hasRoot.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf<NetworkProfile?>(null) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    val activeProfile = profiles.find { it.isActive }

    Scaffold(
        containerColor = DarkBg,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = DarkCard,
                    contentColor = TextPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEdit(null) },
                containerColor = StarBlue,
                contentColor = DarkBg,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "添加配置")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题栏
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "⚡ StarGate",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "一键切换网络配置",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Row {
                        // 调试按钮
                        IconButton(onClick = onNavigateToDebug) {
                            Icon(
                                Icons.Rounded.BugReport,
                                contentDescription = "诊断",
                                tint = TextMuted
                            )
                        }
                        // 刷新按钮
                        IconButton(onClick = { viewModel.refreshNetworkStatus() }) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = "刷新",
                                tint = TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Root 权限提示
            if (hasRoot == false) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = StatusRed.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.Warning, null, tint = StatusRed)
                            Column {
                                Text(
                                    "未获取 Root 权限",
                                    color = StatusRed,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "请在 APatch/Magisk 中为 StarGate 授权 Root",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // 网络状态卡片
            item {
                StatusCard(
                    status = networkStatus,
                    activeProfileName = activeProfile?.name
                )
            }

            // 正在切换中的提示
            if (isSwitching) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = StarBlue.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = StarBlue,
                                strokeWidth = 2.dp
                            )
                            Text(
                                "正在切换网络配置...",
                                color = StarBlue,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // 配置列表标题
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "网络配置",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${profiles.size} 个配置",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // 空状态
            if (profiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Rounded.WifiFind,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = TextMuted
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "还没有配置",
                                color = TextSecondary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "点击右下角 + 添加你的第一个网络配置",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // 配置卡片列表
            items(profiles, key = { it.id }) { profile ->
                ProfileCard(
                    profile = profile,
                    onSwitch = {
                        if (!isSwitching) {
                            viewModel.switchProfile(profile)
                        }
                    },
                    onEdit = { onNavigateToEdit(profile.id) },
                    onDelete = { showDeleteDialog = profile }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // 删除确认对话框
    showDeleteDialog?.let { profile ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = DarkSurface,
            title = { Text("删除配置", color = TextPrimary) },
            text = {
                Text(
                    "确定要删除「${profile.name}」吗？此操作不可撤销。",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProfile(profile.id)
                    showDeleteDialog = null
                }) {
                    Text("删除", color = StatusRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}
